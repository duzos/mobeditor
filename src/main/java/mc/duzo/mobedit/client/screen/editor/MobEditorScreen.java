package mc.duzo.mobedit.client.screen.editor;

import mc.duzo.mobedit.MobEditMod;
import mc.duzo.mobedit.client.MobEditModClient;
import mc.duzo.mobedit.client.screen.ScreenHelper;
import mc.duzo.mobedit.client.screen.editor.child.ManageDropScreen;
import mc.duzo.mobedit.client.screen.editor.child.ManageEnchantScreen;
import mc.duzo.mobedit.client.screen.widget.NumericalEditBoxWidget;
import mc.duzo.mobedit.client.screen.widget.ScrollableButton;
import mc.duzo.mobedit.client.screen.widget.ScrollableButtonsWidget;
import mc.duzo.mobedit.common.edits.attribute.applier.ApplierRegistry;
import mc.duzo.mobedit.common.edits.attribute.applier.AttributeApplier;
import mc.duzo.mobedit.common.edits.attribute.holder.AttributeHolder;
import mc.duzo.mobedit.common.edits.edited.EditedEntity;
import mc.duzo.mobedit.network.MobEditNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.client.screen.ButtonList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;

public class MobEditorScreen extends Screen {
	private static final Identifier TEXTURE = new Identifier(MobEditMod.MOD_ID, "textures/gui/editor.png");

	private EditedEntity editor;
	private boolean wasPreviousNext;
	private HashMap<String, NumericalEditBoxWidget> editBoxes;
	private EditBoxWidget nameBox;
	private ScrollableButtonsWidget entityButtons;
	private ScrollableButtonsWidget savedButtons;
	private PressableTextWidget deleteButton;

	public MobEditorScreen() {
		super(Text.translatable("screen." + MobEditMod.MOD_ID + ".mob_editor"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBGTexture(context);

		super.render(context, mouseX, mouseY, delta);

		this.renderEntity(context, mouseX, mouseY);

		// this might cause lag
		for (String name : this.editBoxes.keySet()) {
			int y = this.editBoxes.get(name).getY();

			ScreenHelper.renderWidthScaledText(
					name,
					context,
					ScreenHelper.getCentreX() + 16 - 4 - this.textRenderer.getWidth(name),
					y + 5,
					0xFFFFFF,
					this.textRenderer.getWidth(name),
					false
			);
		}
	}

	public static void renderBGTexture(DrawContext context) {
		context.drawTexture(TEXTURE, ScreenHelper.getCentreX() - (384 / 2), ScreenHelper.getCentreY() - (166 / 2), 0, 0, 384, 384, 384, 384); // Background texture
	}

	@Override
	protected void init() {
		super.init();

		boolean edited = this.editor != null;
		if (!edited)
			this.editor = new EditedEntity(0);

		/*
		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("→"), (widget) -> this.selectNextEntity(), ScreenHelper.getCentreX() + 64 + 16, ScreenHelper.getCentreY(), false)
		);

		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("←"), (widget) -> this.selectPreviousEntity(), ScreenHelper.getCentreX() + 64 - this.textRenderer.getWidth("←") - 16, ScreenHelper.getCentreY(), false)
		);
		 */

		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("CREATE"), (widget) -> this.pressComplete(), ScreenHelper.getCentreX() + 128, ScreenHelper.getCentreY() + 32, true)
		);

		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("SAVE"), (widget) -> this.pressSave(), ScreenHelper.getCentreX() + 128, ScreenHelper.getCentreY() + 48, true)
		);

		this.deleteButton = ScreenHelper.createTextButton(this.textRenderer, Text.of("DELETE"), (widget) -> this.pressDelete(), ScreenHelper.getCentreX() + 128, ScreenHelper.getCentreY() + 62, true);  
		this.deleteButton.visible = false;
		this.addDrawableChild(deleteButton);

		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("MANAGE ENCHANT"), (widget) -> this.pressAddEnchant(), ScreenHelper.getCentreX() + 16, ScreenHelper.getCentreY() + 48, true)
		);
		this.addDrawableChild(
				ScreenHelper.createTextButton(this.textRenderer, Text.of("MANAGE DROPS"), (widget) -> this.pressAddDrop(), ScreenHelper.getCentreX() + 16, ScreenHelper.getCentreY() + 64, true)
		);

		this.nameBox = new EditBoxWidget(this.textRenderer, ScreenHelper.getCentreX() + 80, ScreenHelper.getCentreY() - 9, 96, 18, Text.of(""), Text.of("NAME"));
		this.addDrawableChild(this.nameBox);

		this.editBoxes = new HashMap<>();

		this.entityButtons = this.createEntityButtonList(ScreenHelper.getCentreX() - 184, ScreenHelper.getCentreY() - 66, 128, 64);
		this.addDrawableChild(this.entityButtons);

		this.savedButtons = this.createSavedEntityList();
		this.addDrawableChild(this.savedButtons);

		int count = 1;
		for (AttributeApplier applier : ApplierRegistry.REGISTRY) {
			createEditBoxForApplier(applier, count);
			count++;
		}

		this.onChangeEntity(edited);
	}

	private NumericalEditBoxWidget createEditBoxForApplier(AttributeApplier applier, int count) {
		boolean even = (count % 2) == 0;
		int y = ScreenHelper.getCentreY() - 16;
		if (even) {
			y = y - (20 * (count / 2));
		}
		else {
			y = y + (20 * (count / 2));
		}

		NumericalEditBoxWidget widget = new NumericalEditBoxWidget(
				this.textRenderer,
				ScreenHelper.getCentreX() + 16,
				y,
				48,
				18,
				0,
				Text.of("")
		);

		this.addDrawableChild(widget);

		this.editBoxes.put(applier.getName(), widget);

		return widget;
	}
	private LivingEntity getSelectedEntity() {
		LivingEntity found = editor.getSelectedEntity(MinecraftClient.getInstance().world).orElse(null);

		if (found == null) {
			if (this.wasPreviousNext) this.selectNextEntity();
			else this.selectPreviousEntity();

			return this.getSelectedEntity();
		}

		return found;
	}

	public EditedEntity getEditor() {
		return editor;
	}

	private void selectNextEntity() {
		int index = this.editor.getEntityIndex();

		index++;

		if (index > getRegistrySize()) {
			index = 0;
		}

		this.editor = new EditedEntity(index);
		this.wasPreviousNext = true;
		this.onChangeEntity();
	}
	private void selectPreviousEntity() {
		int index = this.editor.getEntityIndex();

		index--;

		if (index < 0) {
			index = getRegistrySize();
		}

		this.editor = new EditedEntity(index);
		this.wasPreviousNext = false;
		this.onChangeEntity();
	}
	public void onChangeEntity(boolean isEdited) {
		for (String name : this.editBoxes.keySet()) {
			NumericalEditBoxWidget widget = this.editBoxes.get(name);
			AttributeApplier applier = applierFromName(name);

			double value;
			if (!isEdited) {
				value = MobEditMod.round(applier.getDefault(this.getSelectedEntity()).orElse(0d), 2);
			} else {
				AttributeHolder holder = this.editor.getAttribute(applier).orElse(null);
				if (holder == null) {
					value = MobEditMod.round(applier.getDefault(this.getSelectedEntity()).orElse(0d), 2);
				} else {
					value = holder.getTarget();
				}
			}

			widget.setText("" + value);
		}

		this.nameBox.setText(this.editor.getName().orElse(""));
		this.deleteButton.visible = isEdited;
	}
	private void onChangeEntity() {
		this.onChangeEntity(false);
	}

	private static int getRegistrySize() {
		return Registries.ENTITY_TYPE.size();
	}

	private void renderEntity(DrawContext context, float mouseX, float mouseY) {
		int x = ScreenHelper.getCentreX() + 76;
		int y = ScreenHelper.getCentreY() - 64;

		InventoryScreen.drawEntity(context, x, y, x + 105, y + 49, 28, 0.0625F, mouseX, mouseY, this.getSelectedEntity());

		ScreenHelper.renderWidthScaledText(
				this.getSelectedEntity().getName().getString(),
				context,
				ScreenHelper.getCentreX() + 128,
				ScreenHelper.getCentreY() + 16,
				0xFFFFFF,
				this.textRenderer.getWidth(this.getSelectedEntity().getName()),
				true
		);

	}

	private void pressComplete() {
		this.setToEntity();

		NbtCompound data = this.editor.serialize();

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeNbt(data);

		ClientPlayNetworking.send(MobEditNetworking.REQUEST_EGG, buf);

		this.close();
	}
	private void setToEntity() {
		this.editor.getAttributes().clear();

		for (String name : this.editBoxes.keySet()) {
			NumericalEditBoxWidget widget = this.editBoxes.get(name);
			AttributeApplier applier = applierFromName(name);
			if (widget.getValue() == applier.getDefault(this.getSelectedEntity()).orElse(-1d)) continue;

			this.editor.addAttribute(new AttributeHolder(applier, widget.getValue()));
		}

		if (!this.nameBox.getText().isBlank()) {
			this.editor.setName(this.nameBox.getText());
		}
	}

	private AttributeApplier applierFromName(String name) {
		return ApplierRegistry.REGISTRY.stream().filter(applier -> applier.getName().equals(name)).findFirst().orElse(null);
	}

	private ScrollableButtonsWidget createEntityButtonList(int x, int y, int width, int height) {
		ButtonList list = new ButtonList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

		int index = 0;
		for (EntityType<?> type : Registries.ENTITY_TYPE) {
			list.add(createEntityButton(type, index, x, y, width, height / 4));
			index++;
		}

		ScrollableButtonsWidget created = new ScrollableButtonsWidget(x, y, width, height, Text.of("Entities"), list);

		for (ClickableWidget i : list) { // code bad
			if (!(i instanceof ScrollableButton button)) continue;

			button.setParent(created);
		}

		return created;
	}
	private ScrollableButton createEntityButton(EntityType<?> type, int index, int x, int y, int width, int height) {
		return ScrollableButton.builder(
				type.getName(),
				button -> {
					System.out.println("PRESSED " + button.getMessage().getString());
					this.editor = new EditedEntity(index);
					this.onChangeEntity();
				},
				null
		).dimensions(x, y, width, height).build();
	}

	private ScrollableButtonsWidget createSavedEntityList(int x, int y, int width, int height) {
		ButtonList list = new ButtonList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

		int index = 0;
		for (EditedEntity entity : MobEditModClient.editedEntities.list) {
			list.add(createSavedEntityButton(entity, index, x, y, width, height / 4));
			index++;
		}

		ScrollableButtonsWidget created = new ScrollableButtonsWidget(x, y, width, height, Text.of("Entities"), list);

		for (ClickableWidget i : list) { // code bad
			if (!(i instanceof ScrollableButton button)) continue;

			button.setParent(created);
		}

		return created;
	}

	private ScrollableButtonsWidget createSavedEntityList() {
		return this.createSavedEntityList(ScreenHelper.getCentreX() - 184, ScreenHelper.getCentreY() + 2, 128, 64);
	}

	private ScrollableButton createSavedEntityButton(EditedEntity entity, int index, int x, int y, int width, int height) {
		return ScrollableButton.builder(
				Text.of(entity.getName().orElse("No Name")),
				button -> {
					System.out.println("PRESSED " + button.getMessage().getString());
					this.editor = entity;
					this.onChangeEntity(true);
				},
				null
		).dimensions(x, y, width, height).build();
	}


	private void pressSave() {
		this.setToEntity();
		MobEditModClient.editedEntities.list.add(this.editor);
		this.savedButtons = this.createSavedEntityList();

		// button dont refresh, give up
		this.close();
	}
	private void pressDelete() {
		MobEditModClient.editedEntities.remove(MobEditModClient.getClientSavePath(), this.editor);
		this.editor = new EditedEntity(0);
		this.savedButtons = this.createSavedEntityList();

		// buttons dont refresh, give up
		this.close();
	}

	private void pressAddEnchant() {
		this.setToEntity();
		ScreenHelper.setScreen(new ManageEnchantScreen(this));
	}
	private void pressAddDrop() {
		this.setToEntity();
		ScreenHelper.setScreen(new ManageDropScreen(this));
	}

	@Override
	public boolean shouldPause() {
		return true;
	}
}

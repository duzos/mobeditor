package mc.duzo.mobedit.client.screen.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ScrollableButton extends ButtonWidget {
	private ScrollableButtonsWidget parent;

	protected ScrollableButton(int x, int y, int width, int height, Text message, PressAction onPress, NarrationSupplier narrationSupplier, @Nullable ScrollableButtonsWidget parent) {
		super(x, y, width, height, message, onPress, narrationSupplier);

		this.parent = parent;
	}

	public ScrollableButtonsWidget getParent() {
		return parent;
	}

	public void setParent(ScrollableButtonsWidget parent) {
		this.parent = parent;
	}

	@Override
	public boolean clicked(double mouseX, double mouseY) {
		if (this.parent == null) return super.isMouseOver(mouseX, mouseY);
		if (!this.isVisible()) return false;

		return this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getOffsetY() && mouseX < this.getX() + this.width && mouseY < this.getOffsetY() + this.height;
	}

	public int getParentIndex() {
		return this.getParent().getButtons().indexOf(this);
	}
	private double getOffsetY() {
		return this.getY() - this.getParent().getScrollY();
	}
	private boolean isVisible() {
		return !(this.getOffsetY() < this.getParent().getY() || this.getOffsetY() > (this.getParent().getY() + this.getParent().getHeight()));
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!this.visible) {
			return;
		}
		this.hovered = clicked(mouseX, mouseY);
		super.renderWidget(context, mouseX, mouseY, delta);
	}

	public static class Builder {

		private final Text message;
		private final PressAction onPress;
		@Nullable
		private Tooltip tooltip;
		private int x;
		private int y;
		private int width = 150;
		private int height = 20;
		private NarrationSupplier narrationSupplier = DEFAULT_NARRATION_SUPPLIER;
		private ScrollableButtonsWidget parent;

		public Builder(Text message, PressAction onPress, ScrollableButtonsWidget parent) {
			this.message = message;
			this.onPress = onPress;
			this.parent = parent;
		}

		public Builder position(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public Builder width(int width) {
			this.width = width;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder dimensions(int x, int y, int width, int height) {
			return this.position(x, y).size(width, height);
		}

		public Builder tooltip(@Nullable Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Builder narrationSupplier(NarrationSupplier narrationSupplier) {
			this.narrationSupplier = narrationSupplier;
			return this;
		}

		public ScrollableButton build() {
			ScrollableButton buttonWidget = new ScrollableButton(this.x, this.y, this.width, this.height, this.message, this.onPress, this.narrationSupplier, this.parent);
			buttonWidget.setTooltip(this.tooltip);
			return buttonWidget;
		}
	}
	public static Builder builder(Text message, PressAction onPress, @Nullable ScrollableButtonsWidget parent) {
		return new Builder(message, onPress, parent);
	}
}

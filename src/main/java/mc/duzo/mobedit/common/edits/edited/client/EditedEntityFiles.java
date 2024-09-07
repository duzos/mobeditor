package mc.duzo.mobedit.common.edits.edited.client;

import mc.duzo.mobedit.MobEditMod;
import mc.duzo.mobedit.common.edits.edited.EditedEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EditedEntityFiles {
	public final ArrayList<EditedEntity> list;

	public EditedEntityFiles(@Nullable Path root) {
		this.list = new ArrayList<>();

		if (root != null) this.readFiles(root);
	}

	private void readFiles(Path root) {
		this.list.clear();

		try {
			for (String fileName : getFilesInPath(getSavePath(root))) {
				Path file = getSavePath(root).resolve(fileName);
				NbtCompound data = NbtIo.read(file);
				if (data == null) continue;

				EditedEntity entity = new EditedEntity(data);
				if (!entity.isValid()) continue;
				this.list.add(entity);
			}
		} catch (IOException e) {
			MobEditMod.LOGGER.error("Failed to read edited entities", e);
		}
	}
	public void writeToFiles(Path root) {
		try {
			for (EditedEntity entity : this.list) {
				NbtIo.write(entity.serialize(), getSavePath(root).resolve(entity.getUuid().toString() + ".dat"));
			}
		} catch (IOException e) {
			MobEditMod.LOGGER.error("Failed to write edited entities", e);
		}
	}

	private Set<String> getFilesInPath(Path path) throws IOException {
		try (Stream<Path> stream = Files.list(path)) {
			return stream
					.filter(file -> !Files.isDirectory(file))
					.map(Path::getFileName)
					.map(Path::toString)
					.collect(Collectors.toSet());
		}
	}

	private Path getSavePath(Path root) {
		Path save = root.resolve(MobEditMod.MOD_ID).resolve("saves");

		try {
			Files.createDirectories(save);
		} catch (IOException e) {
			MobEditMod.LOGGER.error("Failed to create save directory", e);
		}

		return save;
	}

	public void remove(Path root, EditedEntity editor) {
		if (!this.list.contains(editor)) return;

		this.list.remove(editor);

		File possible = getSavePath(root).resolve(editor.getUuid().toString() + ".dat").toFile();
		if (possible.exists()) {
			possible.delete();
		}
	}
}

package cn.racyacid.gimeachoi.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sun.jna.platform.FileUtils;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.SessionLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@Mixin(LevelStorage.Session.class)
public class SessionMixin {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("GimmeAChoice");

    @Shadow @Final SessionLock lock;
    @Shadow @Final LevelStorage.LevelSave directory;

    @ModifyArg(method = "deleteSessionLock", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;walkFileTree(Ljava/nio/file/Path;Ljava/nio/file/FileVisitor;)Ljava/nio/file/Path;"), index = 1)
    private FileVisitor<? super Path> dontDeleteButMove2RecycleBin(FileVisitor<? super Path> visitor, @Local Path path) {
        FileUtils fileUtils = FileUtils.getInstance();

        return new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes basicFileAttributes) throws IOException {
                if (!file.equals(path)) {
                    LOGGER.debug("Moving \"{}\" to recycle bin", file);
                    fileUtils.moveToTrash(file.toFile());
                }

                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException iOException) throws IOException {
                if (iOException != null) throw iOException;

                if (dir.equals(directory.path())) {
                    lock.close();
                    fileUtils.moveToTrash(dir.toFile());
                }

                if (path.toFile().exists()) fileUtils.moveToTrash(path.toFile());
                return FileVisitResult.CONTINUE;
            }
        };
    }
}
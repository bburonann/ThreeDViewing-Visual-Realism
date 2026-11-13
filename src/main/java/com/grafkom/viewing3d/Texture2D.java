package com.grafkom.viewing3d;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class Texture2D {

    private int textureID;

    public Texture2D(String path) {

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        STBImage.stbi_set_flip_vertically_on_load(true);

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load(
                    path, width, height, channels, 4
            );

            if (image == null) {
                throw new RuntimeException(
                        "Texture gagal dimuat: " + path
                        + " - " + STBImage.stbi_failure_reason()
                );
            }

            try {
                glTexImage2D(
                        GL_TEXTURE_2D,
                        0,
                        GL_RGBA8,
                        width.get(0),
                        height.get(0),
                        0,
                        GL_RGBA,
                        GL_UNSIGNED_BYTE,
                        image
                );

                glGenerateMipmap(GL_TEXTURE_2D);
            } finally {
                STBImage.stbi_image_free(image);
            }
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        setFiltering(true);
    }

    public void setFiltering(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                mipmapped ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST
        );

        glTexParameteri(
                GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER,
                mipmapped ? GL_LINEAR : GL_NEAREST
        );
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}

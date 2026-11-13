package com.grafkom.viewing3d;

import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh3D {

    private int cubeVAO, cubeVBO;
    private int planeVAO, planeVBO;

    private boolean initialized;

    // POSITION(3), FACE NORMAL(3), SMOOTH NORMAL(3), UV(2)
    private final float[] cubeVertices = {
        // FRONT
        -1,-1,1,  0,0,1,  -0.577f,-0.577f,0.577f, 0,0,
         1,-1,1,  0,0,1,   0.577f,-0.577f,0.577f, 1,0,
         1,1,1,   0,0,1,   0.577f,0.577f,0.577f,  1,1,
        -1,-1,1,  0,0,1,  -0.577f,-0.577f,0.577f, 0,0,
         1,1,1,   0,0,1,   0.577f,0.577f,0.577f,  1,1,
        -1,1,1,   0,0,1,  -0.577f,0.577f,0.577f,  0,1,

        // BACK
        -1,-1,-1, 0,0,-1, -0.577f,-0.577f,-0.577f, 1,0,
        -1,1,-1,  0,0,-1, -0.577f,0.577f,-0.577f,  1,1,
         1,1,-1,  0,0,-1,  0.577f,0.577f,-0.577f,  0,1,
        -1,-1,-1, 0,0,-1, -0.577f,-0.577f,-0.577f, 1,0,
         1,1,-1,  0,0,-1,  0.577f,0.577f,-0.577f,  0,1,
         1,-1,-1, 0,0,-1,  0.577f,-0.577f,-0.577f, 0,0,

        // RIGHT
         1,-1,1,  1,0,0,  0.577f,-0.577f,0.577f,  0,0,
         1,-1,-1, 1,0,0,  0.577f,-0.577f,-0.577f, 1,0,
         1,1,-1,  1,0,0,  0.577f,0.577f,-0.577f,  1,1,
         1,-1,1,  1,0,0,  0.577f,-0.577f,0.577f,  0,0,
         1,1,-1,  1,0,0,  0.577f,0.577f,-0.577f,  1,1,
         1,1,1,  1,0,0,  0.577f,0.577f,0.577f,   0,1,

        // LEFT
        -1,-1,-1, -1,0,0, -0.577f,-0.577f,-0.577f, 0,0,
        -1,-1,1,  -1,0,0, -0.577f,-0.577f,0.577f,  1,0,
        -1,1,1,   -1,0,0, -0.577f,0.577f,0.577f,   1,1,
        -1,-1,-1, -1,0,0, -0.577f,-0.577f,-0.577f, 0,0,
        -1,1,1,   -1,0,0, -0.577f,0.577f,0.577f,   1,1,
        -1,1,-1,  -1,0,0, -0.577f,0.577f,-0.577f,  0,1,

        // TOP
        -1,1,1,  0,1,0, -0.577f,0.577f,0.577f,  0,0,
         1,1,1,  0,1,0,  0.577f,0.577f,0.577f,  1,0,
         1,1,-1, 0,1,0,  0.577f,0.577f,-0.577f, 1,1,
        -1,1,1,  0,1,0, -0.577f,0.577f,0.577f,  0,0,
         1,1,-1, 0,1,0,  0.577f,0.577f,-0.577f, 1,1,
        -1,1,-1, 0,1,0, -0.577f,0.577f,-0.577f, 0,1,

        // BOTTOM
        -1,-1,-1, 0,-1,0, -0.577f,-0.577f,-0.577f, 0,0,
         1,-1,-1, 0,-1,0,  0.577f,-0.577f,-0.577f, 1,0,
         1,-1,1,  0,-1,0,  0.577f,-0.577f,0.577f,  1,1,
        -1,-1,-1, 0,-1,0, -0.577f,-0.577f,-0.577f, 0,0,
         1,-1,1,  0,-1,0,  0.577f,-0.577f,0.577f,  1,1,
        -1,-1,1,  0,-1,0, -0.577f,-0.577f,0.577f, 0,1
    };

    // Ground plane: 10 x 10
    private final float[] planeVertices = {
        -5,0,-5,  0,1,0,  0,1,0,  0,0,
         5,0,-5,  0,1,0,  0,1,0,  5,0,
         5,0, 5,  0,1,0,  0,1,0,  5,5,

        -5,0,-5,  0,1,0,  0,1,0,  0,0,
         5,0, 5,  0,1,0,  0,1,0,  5,5,
        -5,0, 5,  0,1,0,  0,1,0,  0,5
    };

    public void init() {
        cubeVAO = createMesh(cubeVertices);
        planeVAO = createMesh(planeVertices);
        initialized = true;
    }

    private int createMesh(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);

        try {
            buffer.put(data).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }

        int stride = 11 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride,
                3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride,
                6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 2, GL_FLOAT, false, stride,
                9L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);

        return vao;
    }

    public void drawCube() {
        if (!initialized) throw new IllegalStateException("Mesh not initialized");
        glBindVertexArray(cubeVAO);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);
    }

    public void drawGround() {
        if (!initialized) throw new IllegalStateException("Mesh not initialized");
        glBindVertexArray(planeVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (initialized) {
            glDeleteVertexArrays(cubeVAO);
            glDeleteVertexArrays(planeVAO);
            initialized = false;
        }
    }
}

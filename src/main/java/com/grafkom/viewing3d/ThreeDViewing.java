package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ThreeDViewing {

    private long window;

    private final Camera3D camera = new Camera3D();
    private final Mesh3D mesh = new Mesh3D();

    private ShaderProgram shader;
    private ShaderProgram shadowShader;

    private ShadowMapFBO shadowFBO;
    private Texture2D texture;

    private int width = 1000;
    private int height = 700;

    private boolean mousePressed;
    private double lastX, lastY;

    private boolean blinnPhong;
    private boolean smoothShading;
    private boolean depthEnabled = true;
    private boolean cullingEnabled;

    private boolean textureEnabled = true;
    private boolean mipmapped = true;
    private boolean shadowEnabled = true;

    private final Vector3f lightPosition =
            new Vector3f(3f, 5f, 3f);

    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {

        if (!glfwInit()) {
            throw new RuntimeException("GLFW initialization failed");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(
                width,
                height,
                "3D Visual Realism Suite - Week 11",
                0, 0
        );

        if (window == 0) {
            throw new RuntimeException("Window creation failed");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        GL.createCapabilities();

        System.out.println("OpenGL: " + glGetString(GL_VERSION));

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glClearColor(0.07f, 0.08f, 0.12f, 1f);

        mesh.init();

        shader = new ShaderProgram(
                "/shaders/phong.vert",
                "/shaders/phong.frag"
        );

        shadowShader = new ShaderProgram(
                "/shaders/shadow.vert",
                "/shaders/shadow.frag"
        );

        shadowFBO = new ShadowMapFBO();

        texture = new Texture2D(
                "src/main/resources/textures/brick.jpg"
        );

        setupCallbacks();
    }

    private void setupCallbacks() {

        glfwSetFramebufferSizeCallback(window, (w, newWidth, newHeight) -> {
            width = newWidth;
            height = Math.max(newHeight, 1);
            glViewport(0, 0, width, height);
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {

            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = action == GLFW_PRESS;

                if (mousePressed) {
                    double[] x = new double[1];
                    double[] y = new double[1];

                    glfwGetCursorPos(window, x, y);

                    lastX = x[0];
                    lastY = y[0];
                }
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {

            if (mousePressed) {
                camera.rotate(
                        (float) (x - lastX),
                        (float) (y - lastY)
                );

                lastX = x;
                lastY = y;
            }
        });

        glfwSetScrollCallback(window, (w, x, y) ->
                camera.zoom((float) y * 0.4f)
        );

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {

            if (action != GLFW_PRESS) return;

            switch (key) {

                case GLFW_KEY_ESCAPE ->
                        glfwSetWindowShouldClose(window, true);

                case GLFW_KEY_B -> {
                    blinnPhong = !blinnPhong;
                    printStatus("Shading",
                            blinnPhong ? "Blinn-Phong" : "Phong");
                }

                case GLFW_KEY_F -> {
                    smoothShading = !smoothShading;
                    printStatus("Surface",
                            smoothShading ? "Smooth" : "Flat");
                }

                case GLFW_KEY_Z -> {
                    depthEnabled = !depthEnabled;

                    if (depthEnabled) glEnable(GL_DEPTH_TEST);
                    else glDisable(GL_DEPTH_TEST);

                    printStatus("Z-Buffer", depthEnabled);
                }

                case GLFW_KEY_C -> {
                    cullingEnabled = !cullingEnabled;

                    if (cullingEnabled) {
                        glEnable(GL_CULL_FACE);
                        glCullFace(GL_BACK);
                        glFrontFace(GL_CCW);
                    } else {
                        glDisable(GL_CULL_FACE);
                    }

                    printStatus("Culling", cullingEnabled);
                }

                case GLFW_KEY_T -> {
                    textureEnabled = !textureEnabled;
                    printStatus("Texture", textureEnabled);
                }

                case GLFW_KEY_N -> {
                    mipmapped = !mipmapped;
                    texture.setFiltering(mipmapped);
                    printStatus("Texture filtering",
                            mipmapped ? "Bilinear Mipmapped" : "Nearest");
                }

                case GLFW_KEY_H -> {
                    shadowEnabled = !shadowEnabled;
                    printStatus("Shadow Mapping", shadowEnabled);
                }
            }
        });
    }

    private Matrix4f getLightSpaceMatrix() {

        Matrix4f lightProjection = new Matrix4f()
                .ortho(-8f, 8f, -8f, 8f, 1f, 20f);

        Matrix4f lightView = new Matrix4f()
                .lookAt(
                        lightPosition,
                        new Vector3f(0, 0, 0),
                        new Vector3f(0, 1, 0)
                );

        return lightProjection.mul(lightView);
    }

    private Matrix4f cubeModel() {
        return new Matrix4f()
                .translate(0f, -0.25f, 0f)
                .rotateY((float) (glfwGetTime() * 0.5))
                .scale(0.75f);
    }

    private Matrix4f groundModel() {
        return new Matrix4f()
                .translate(0f, -1.05f, 0f);
    }

    private void loop() {

        while (!glfwWindowShouldClose(window)) {

            Matrix4f lightSpaceMatrix = getLightSpaceMatrix();

            // PASS 1: SHADOW DEPTH RENDERING
            if (shadowEnabled) {

                shadowFBO.bindForWriting();

                glClear(GL_DEPTH_BUFFER_BIT);

                glEnable(GL_DEPTH_TEST);

                // Avoid face-culling issues in the shadow pass
                glDisable(GL_CULL_FACE);

                shadowShader.use();

                int shadowID = shadowShader.getID();

                setMatrix(
                        shadowID,
                        "lightSpaceMatrix",
                        lightSpaceMatrix
                );

                Matrix4f cubeModel = cubeModel();

                setMatrix(shadowID, "model", cubeModel);
                mesh.drawCube();

                setMatrix(
                        shadowID,
                        "model",
                        groundModel()
                );

                mesh.drawGround();

                shadowFBO.unbind(width, height);
            }

            // PASS 2: CAMERA / LIGHTING RENDERING
            glViewport(0, 0, width, height);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (depthEnabled) glEnable(GL_DEPTH_TEST);
            else glDisable(GL_DEPTH_TEST);

            if (cullingEnabled) {
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
                glFrontFace(GL_CCW);
            } else {
                glDisable(GL_CULL_FACE);
            }

            Matrix4f view = camera.calculateLookAtMatrix();

            Matrix4f projection = new Matrix4f()
                    .perspective(
                            (float) Math.toRadians(45),
                            (float) width / height,
                            0.1f,
                            100f
                    );

            shader.use();

            int id = shader.getID();

            setMatrix(id, "view", view);
            setMatrix(id, "projection", projection);
            setMatrix(id, "lightSpaceMatrix", lightSpaceMatrix);

            setVector(id, "lightPos",
                    lightPosition.x,
                    lightPosition.y,
                    lightPosition.z);

            Vector3f eye = camera.getEye();

            setVector(id, "viewPos", eye.x, eye.y, eye.z);
            setVector(id, "lightColor", 1f, 1f, 1f);

            glUniform1i(
                    glGetUniformLocation(id, "useBlinnPhong"),
                    blinnPhong ? 1 : 0
            );

            glUniform1i(
                    glGetUniformLocation(id, "useSmooth"),
                    smoothShading ? 1 : 0
            );

            glUniform1i(
                    glGetUniformLocation(id, "useTexture"),
                    textureEnabled ? 1 : 0
            );

            glUniform1i(
                    glGetUniformLocation(id, "useShadow"),
                    shadowEnabled ? 1 : 0
            );

            glUniform1i(
                    glGetUniformLocation(id, "textureSampler"),
                    0
            );

            glUniform1i(
                    glGetUniformLocation(id, "shadowMap"),
                    1
            );

            // Draw textured cube
            setMatrix(id, "model", cubeModel());
            setVector(id, "objectColor", 0.85f, 0.85f, 0.85f);

            texture.bind(0);

            glActiveTexture(GL_TEXTURE1);
            glBindTexture(
                    GL_TEXTURE_2D,
                    shadowFBO.getDepthTextureID()
            );

            mesh.drawCube();

            // Draw ground plane
            setMatrix(id, "model", groundModel());
            setVector(id, "objectColor", 0.45f, 0.48f, 0.52f);

            glUniform1i(
                    glGetUniformLocation(id, "useTexture"),
                    0
            );

            mesh.drawGround();

            glBindVertexArray(0);
            glUseProgram(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void setMatrix(
            int program,
            String name,
            Matrix4f matrix) {

        try (org.lwjgl.system.MemoryStack stack =
                org.lwjgl.system.MemoryStack.stackPush()) {

            glUniformMatrix4fv(
                    glGetUniformLocation(program, name),
                    false,
                    matrix.get(stack.mallocFloat(16))
            );
        }
    }

    private void setVector(
            int program,
            String name,
            float x,
            float y,
            float z) {

        glUniform3f(
                glGetUniformLocation(program, name),
                x, y, z
        );
    }

    private void printStatus(String feature, Object value) {
        System.out.println(feature + ": " + value);
    }

    private void cleanup() {

        if (texture != null) texture.cleanup();
        if (shadowFBO != null) shadowFBO.cleanup();

        mesh.cleanup();

        if (shader != null) shader.delete();
        if (shadowShader != null) shadowShader.delete();

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}

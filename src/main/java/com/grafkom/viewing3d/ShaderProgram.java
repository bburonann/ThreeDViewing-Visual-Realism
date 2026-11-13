package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final int programID;

    public ShaderProgram(String vertexPath, String fragmentPath) {

        String vertexSource = load(vertexPath);
        String fragmentSource = load(fragmentPath);

        int vertexShader = compile(vertexSource, GL_VERTEX_SHADER);
        int fragmentShader = compile(fragmentSource, GL_FRAGMENT_SHADER);

        programID = glCreateProgram();

        glAttachShader(programID, vertexShader);
        glAttachShader(programID, fragmentShader);

        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                "Shader linking failed: " + glGetProgramInfoLog(programID)
            );
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private String load(String path) {

        try (var input = getClass().getResourceAsStream(path)) {

            if (input == null) {
                throw new RuntimeException("Shader not found: " + path);
            }

            return new String(
                input.readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int compile(String source, int type) {

        int shader = glCreateShader(type);

        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                "Shader compilation failed: " + glGetShaderInfoLog(shader)
            );
        }

        return shader;
    }

    public void use() {
        glUseProgram(programID);
    }

    public int getID() {
        return programID;
    }

    public void delete() {
        glDeleteProgram(programID);
    }
}

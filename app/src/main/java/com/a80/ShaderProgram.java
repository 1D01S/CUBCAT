package com.a80;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderProgram {
    private static final String TAG = "ShaderProgram";
    private int program;

    // Статический метод для создания шейдерной программы
    public static int create(String vertexShaderCode, String fragmentShaderCode) {
        ShaderProgram shaderProgram = new ShaderProgram();
        return shaderProgram.createProgram(vertexShaderCode, fragmentShaderCode);
    }

    private int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Error creating program.");
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public void use() {
        if (program != 0) {
            GLES20.glUseProgram(program);
        } else {
            Log.e(TAG, "Shader program is not initialized.");
        }
    }

    public int getProgram() {
        return program;
    }

    public void delete() {
        if (program != 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
            Log.d(TAG, "Shader program deleted.");
        }
    }
}

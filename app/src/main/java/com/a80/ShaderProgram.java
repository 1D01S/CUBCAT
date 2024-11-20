//package com.a80;
//
//import android.opengl.GLES20;
//import android.util.Log;
//
//public class ShaderProgram {
//    private static final String TAG = "ShaderProgram";
//    private int program;
//
//    public static int create() {
//        ShaderProgram shaderProgram = new ShaderProgram();
//
//        int vertexShader = VertexShader.load();
//        int fragmentShader = FragmentShader.load();
//
//        if (vertexShader == 0 || fragmentShader == 0) {
//            return 0;
//        }
//
//        shaderProgram.program = GLES20.glCreateProgram();
//        if (shaderProgram.program == 0) {
//            Log.e(TAG, "Error creating program.");
//            return 0;
//        }
//
//        GLES20.glAttachShader(shaderProgram.program, vertexShader);
//        GLES20.glAttachShader(shaderProgram.program, fragmentShader);
//        GLES20.glLinkProgram(shaderProgram.program);
//
//        int[] linkStatus = new int[1];
//        GLES20.glGetProgramiv(shaderProgram.program, GLES20.GL_LINK_STATUS, linkStatus, 0);
//        if (linkStatus[0] == 0) {
//            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(shaderProgram.program));
//            GLES20.glDeleteProgram(shaderProgram.program);
//            return 0;
//        }
//
//        GLES20.glDeleteShader(vertexShader);
//        GLES20.glDeleteShader(fragmentShader);
//
//        return shaderProgram.program;
//    }
//
//    public void use() {
//        if (program != 0) {
//            GLES20.glUseProgram(program);
//        } else {
//            Log.e(TAG, "Shader program is not initialized.");
//        }
//    }
//
//    public int getProgram() {
//        return program;
//    }
//
//    public void delete() {
//        if (program != 0) {
//            GLES20.glDeleteProgram(program);
//            program = 0;
//            Log.d(TAG, "Shader program deleted.");
//        }
//    }
//}

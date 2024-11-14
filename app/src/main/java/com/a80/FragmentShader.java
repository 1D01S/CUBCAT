package com.a80;

import android.opengl.GLES20;
import android.util.Log;

public class FragmentShader {
    private static final String TAG = "FragmentShader";

    public static int load(String shaderCode) {
        int shader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (shader == 0) {
            Log.e(TAG, "Error creating fragment shader");
            return 0;
        }

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compiling fragment shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}

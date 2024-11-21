package com.a80;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Model {
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    public Model(float[] vertices, short[] indices) {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).position(0);

        indexCount = indices.length;
    }

    public void draw(float[] mvpMatrix, boolean wireframeMode) {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "varying vec3 vPositionInSpace;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  vPositionInSpace = vPosition.xyz;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform bool uWireframeMode;" +
                        "varying vec3 vPositionInSpace;" +
                        "void main() {" +
                        "  if (uWireframeMode) {" +
                        "    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);" +
                        "  } else {" +
                        "    float brightness = 0.5 + 0.5 * sin(length(vPositionInSpace) * 5.0);" +
                        "    vec3 baseColor = vec3(0.2, 0.1, 0.4);" +
                        "    vec3 glowColor = vec3(0.7, 0.5, 1.0);" +
                        "    gl_FragColor = vec4(mix(baseColor, glowColor, brightness), 1.0);" +
                        "  }" +
                        "}";

        // Компиляция шейдеров
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Создание программы и привязка шейдеров
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);

        // Передача данных в шейдеры
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int wireframeModeHandle = GLES20.glGetUniformLocation(program, "uWireframeMode");
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(wireframeModeHandle, wireframeMode ? 1 : 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Выбор режима рисования
        if (wireframeMode) {
            GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        } else {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        }

        // Освобождение ресурсов
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDeleteProgram(program);
    }



    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Проверяем статус компиляции шейдера
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            // Если компиляция не удалась, выводим сообщение об ошибке и удаляем шейдер
            String errorLog = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader: " + errorLog);
        }

        return shader;
    }

}

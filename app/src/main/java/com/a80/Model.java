package com.a80;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

// Класс модели, содержащий вершинные данные и методы отрисовки
public class Model {
    private final FloatBuffer vertexBuffer; // Буфер для вершин
    private final ShortBuffer indexBuffer; // Буфер для индексов
    private final int indexCount; // Количество индексов

    // Конструктор класса модели
    public Model(float[] vertices, short[] indices) {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4) // Создаем буфер под вершины
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0); // Добавляем вершины в буфер

        indexBuffer = ByteBuffer.allocateDirect(indices.length * 2) // Создаем буфер под индексы
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).position(0); // Добавляем индексы в буфер

        indexCount = indices.length; // Устанавливаем количество индексов
    }

    // Метод для отрисовки модели
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

        // Создание программы шейдеров
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program); // Используем программу

        // Передача матрицы и цвета в шейдер
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0); // Устанавливаем матрицу

        // Установка цвета для wireframe или стандартного
        if (wireframeMode) {
            GLES20.glLineWidth(6.0f); // Устанавливаем толщину линии для wireframe
            GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 1.0f, 1.0f); // Белый цвет для wireframe
        } else {
            GLES20.glUniform4f(colorHandle, 0.2f, 0.5f, 1.0f, 1.0f); // Стандартный цвет
        }

        // Настройка вершин
        GLES20.glEnableVertexAttribArray(positionHandle); // Включаем атрибут
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer); // Указываем формат вершин

        // Рисуем в зависимости от режима
        if (wireframeMode) {
            // Рисуем только линии
            for (int i = 0; i < indexBuffer.limit(); i += 3) {
                short a = indexBuffer.get(i);
                short b = indexBuffer.get(i + 1);
                short c = indexBuffer.get(i + 2);

                short[] lineIndices = {a, b, b, c, c, a}; // Создаем линии из треугольников
                ShortBuffer lineBuffer = ByteBuffer.allocateDirect(lineIndices.length * 2)
                        .order(ByteOrder.nativeOrder())
                        .asShortBuffer();
                lineBuffer.put(lineIndices).position(0);

                GLES20.glDrawElements(GLES20.GL_LINES, lineIndices.length, GLES20.GL_UNSIGNED_SHORT, lineBuffer); // Рисуем линии
            }
        } else {
            // Рисуем треугольники
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer); // Рисуем треугольники
        }

        GLES20.glDisableVertexAttribArray(positionHandle); // Отключаем атрибут
        GLES20.glDeleteProgram(program); // Очищаем программу
    }

    // Метод для загрузки шейдера
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type); // Создаем шейдер
        GLES20.glShaderSource(shader, shaderCode); // Применяем исходный код шейдера
        GLES20.glCompileShader(shader); // Компилируем шейдер

        // Проверяем статус компиляции шейдера
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            // Если компиляция не удалась, выводим сообщение об ошибке и удаляем шейдер
            String errorLog = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader: " + errorLog);
        }

        return shader; // Возвращаем шейдер
    }
}
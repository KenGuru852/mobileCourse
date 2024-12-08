package com.example.curs_work;

import java.util.List;
import java.util.Scanner;
import java.nio.ByteOrder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;
import android.content.Context;

class TableObjects {
    private FloatBuffer normalBuffer; // буфер нормали
    private FloatBuffer verticesBuffer; // буфер вершин
    private final FloatBuffer colorBuffer; // буфер цвета
    private ShortBuffer facesVertexBuffer; // буфер "передних" вершин
    private ShortBuffer facesNormalBuffer; // буфер "передних" координат нормали
    private final List<String> facesList; // список "передних" координат из объектов

    TableObjects(Context c, float[] color, String ObjName) {
        // Считываем координаты объектов в списки
        facesList = new ArrayList<>();
        List<String> verticesList = new ArrayList<>();
        List<String> normalList = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(c.getAssets().open(ObjName));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("v ")) {
                    verticesList.add(line); // добавляем координаты вершин
                } else if (line.startsWith("f ")) {
                    facesList.add(line); // добавляем координаты вершин
                } else if (line.startsWith("vn ")) {
                    normalList.add(line); // добавляем координаты нормалей
                }
            }

            // Заполняем буферы считанными координатами
            ByteBuffer buffer1 = ByteBuffer.allocateDirect(verticesList.size() * 3 * 4);
            buffer1.order(ByteOrder.nativeOrder());
            verticesBuffer = buffer1.asFloatBuffer();

            ByteBuffer buffer2 = ByteBuffer.allocateDirect(normalList.size() * 3 * 4);
            buffer2.order(ByteOrder.nativeOrder());
            normalBuffer = buffer2.asFloatBuffer();

            ByteBuffer buffer3 = ByteBuffer.allocateDirect(facesList.size() * 3 * 2);
            buffer3.order(ByteOrder.nativeOrder());
            facesVertexBuffer = buffer3.asShortBuffer();

            ByteBuffer buffer4 = ByteBuffer.allocateDirect(facesList.size() * 3 * 2);
            buffer4.order(ByteOrder.nativeOrder());
            facesNormalBuffer = buffer4.asShortBuffer();

            // Парсим координаты в float и размечаем данные буферов последовательно по координатам
            for (String vertex : verticesList) {
                String[] coords = vertex.split(" ");
                float x = Float.parseFloat(coords[1]);
                float y = Float.parseFloat(coords[2]);
                float z = Float.parseFloat(coords[3]);
                verticesBuffer.put(x);
                verticesBuffer.put(y);
                verticesBuffer.put(z);
            }
            verticesBuffer.position(0);

            for (String vertex : normalList) {
                String[] coords = vertex.split(" ");
                float x = Float.parseFloat(coords[1]);
                float y = Float.parseFloat(coords[2]);
                float z = Float.parseFloat(coords[3]);
                normalBuffer.put(x);
                normalBuffer.put(y);
                normalBuffer.put(z);
            }
            normalBuffer.position(0);

            for (String face : facesList) {
                String[] vertexIndices = face.split(" ");
                for (int i = 1; i <= 3; i++) {
                    String[] indices = vertexIndices[i].split("/");
                    short vertexIndex = Short.parseShort(indices[0]);
                    short normalIndex = Short.parseShort(indices[2]);
                    facesVertexBuffer.put((short) (vertexIndex - 1));
                    facesNormalBuffer.put((short) (normalIndex - 1));
                }
            }
            facesVertexBuffer.position(0);
            facesNormalBuffer.position(0);

            verticesList.clear();
            normalList.clear();

            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load model: " + ObjName, e);
        }

        // Размечаем буфер цветов
        float[] colorData = new float[facesList.size() * 4];
        for (int v = 0; v < facesList.size(); v++) {
            colorData[4 * v] = color[0];
            colorData[4 * v + 1] = color[1];
            colorData[4 * v + 2] = color[2];
            colorData[4 * v + 3] = color[3];
        }

        ByteBuffer bColor = ByteBuffer.allocateDirect(colorData.length * 4);
        bColor.order(ByteOrder.nativeOrder());
        colorBuffer = bColor.asFloatBuffer();
        colorBuffer.put(colorData).position(0);
    }

    void render(int positionAttribute, int normalAttribute, int colorAttribute) {
        // Устанавливаем буферы в позицию
        colorBuffer.position(0);
        normalBuffer.position(0);
        verticesBuffer.position(0);
        facesVertexBuffer.position(0);
        facesNormalBuffer.position(0);

        // Определяем массивы для буферов вершин, нормали и цвета и "подключаем" их
        GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false,
                0, verticesBuffer);
        GLES20.glEnableVertexAttribArray(positionAttribute);

        GLES20.glVertexAttribPointer(normalAttribute, 3, GLES20.GL_FLOAT, false,
                0, normalBuffer);
        GLES20.glEnableVertexAttribArray(normalAttribute);

        GLES20.glVertexAttribPointer(colorAttribute, 4, GLES20.GL_FLOAT, false,
                0, colorBuffer);
        GLES20.glEnableVertexAttribArray(colorAttribute);

        // Рисуем треугольниками объекты
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, facesList.size() * 3,
                GLES20.GL_UNSIGNED_SHORT, facesVertexBuffer);
    }
}
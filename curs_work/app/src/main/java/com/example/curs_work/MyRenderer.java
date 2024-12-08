package com.example.curs_work;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyRenderer implements GLSurfaceView.Renderer
{
    private ShadersWork mSimpleShadowProgram;

    private int mActiveProgram;//дескриптор текущей программы

    private final float[] mMVMatrix = new float[16];//перемноженная матрица моделей и отображения
    private final float[] mMVPMatrix = new float[16];//перемноженная матрица моделей, отображения и проекции
    private final float[] mViewMatrix = new float[16];//матрица отображения
    private final float[] mModelMatrix = new float[16];//матрица моделей
    private final float[] mNormalMatrix = new float[16];//матрица нормалей
    private final float[] mLightMvpMatrix = new float[16];//матрица света для перемноженных матриц проекции и отображения
    private final float[] mProjectionMatrix = new float[16];//матрица проекции
    private final float[] mLightPosInEyeSpace = new float[16];//матрица позиции света(?)
    private final float[] mActualLightPosition = new float[4];//матрица текущего расположения света
    private final float[] mLightProjectionMatrix = new float[16];//матрица проекции для света

    private final float[] mLightPosModel = new float[]//вектор света
            {
                    0.1f, 10.0f, 0.1f, 1.0f
            };

    private float s = 0;
    private int mDisplayWidth;
    private int mDisplayHeight;

    /*уникальные переменные и атрибуты для матриц*/
    private int scene_mvMatrixUniform;
    private int scene_lightPosUniform;
    private int scene_mvpMatrixUniform;
    private int scene_normalMatrixUniform;
    private int scene_shadowProjMatrixUniform;

    private int scene_colorAttribute;
    private int scene_normalAttribute;
    private int scene_positionAttribute;

    private final Context c;

    /*объекты на экране*/
    private TableObjects Cup;
    private TableObjects Table;
    private TableObjects Beets;
    private TableObjects Apple;
    private TableObjects Orange;
    private TableObjects Cabbage;

    MyRenderer(Context c)
    {
        this.c = c;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);//очищаем буфер
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);//активируем буфер глубины
        GLES20.glEnable(GLES20.GL_CULL_FACE);//удаляем задние грани

        Apple = new TableObjects(c, new float[]{1f, 0f, 0f, 1.0f}, "orange.obj");
        Cup = new TableObjects(c, new float[]{0.0f, 0.2f, 0.3f, 1.0f}, "cup.obj");
        Table = new TableObjects(c, new float[]{1f, 1f, 1f, 1.0f}, "Table.obj");
        Beets = new TableObjects(c, new float[]{0.5f, 0.0f, 0.5f, 1.0f}, "beets.obj");
        Orange = new TableObjects(c, new float[]{1.0f, 0.5f, 0f, 1.0f}, "orange.obj");
        Apple = new TableObjects(c, new float[]{0.5f, 0.9f, 0.5f, 1.0f}, "cabbage.obj");
        Cabbage = new TableObjects(c, new float[]{1.0f, 0.0f, 0.0f, 1.0f}, "apple.obj");
        /*загрузка и установление шейдеров*/
        String depth_tex_v_with_shadow_shader = "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uMVMatrix;\n" +
                "uniform mat4 uNormalMatrix;\n" +
                "uniform mat4 uShadowProjMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aColor;\n" +
                "attribute vec3 aNormal;\n" +
                "varying vec3 vPosition;      \t\t\n" +
                "varying vec4 vColor;          \t\t\n" +
                "varying vec3 vNormal;\n" +
                "varying vec4 vShadowCoord;\n" +
                "\n" +
                "void main() {\n" +
                "\tvPosition = vec3(uMVMatrix * aPosition);\n" +
                "\tvColor = aColor;\n" +
                "\tvNormal = vec3(uNormalMatrix * vec4(aNormal, 0.0));\n" +
                "\tvShadowCoord = uShadowProjMatrix * aPosition;\n" +
                "\tgl_Position = uMVPMatrix * aPosition;                     \n" +
                "}";
        String depth_tex_f_with_simple_shadow_shader = "precision mediump float;\n" +
                "\n" +
                "uniform vec3 uLightPos;\n" +
                "uniform sampler2D uShadowTexture;\n" +
                "uniform float uxPixelOffset;\n" +
                "uniform float uyPixelOffset;\n" +
                "varying vec3 vPosition;\n" +
                "varying vec4 vColor;\n" +
                "varying vec3 vNormal;\n" +
                "varying vec4 vShadowCoord;\n" +
                "\n" +
                "float shadowSimple(){\n" +
                "\tvec4 shadowMapPosition = vShadowCoord / vShadowCoord.w;\n" +
                "\tfloat distanceFromLight = texture2D(uShadowTexture, shadowMapPosition.st).z;\n" +
                "\tfloat bias = 0.001;\n" +
                "\treturn float(distanceFromLight > shadowMapPosition.z - bias);\n" +
                "}\n" +
                "  \n" +
                "void main() {\n" +
                "\tvec3 lightVec = uLightPos - vPosition;\n" +
                "\tlightVec = normalize(lightVec);\n" +
                "\tfloat specular = pow(max(dot(vNormal, lightVec), 0.0), 5.0);\n" +
                "\tfloat diffuse = max(dot(vNormal, lightVec), 0.1);\n" +
                "\tfloat ambient = 0.3;\n" +
                "   \tfloat shadow = 1.0;\n" +
                "\t\tif (vShadowCoord.w > 0.0) {\n" +
                "\t\t\tshadow = shadowSimple();\n" +
                "\t\t\tshadow = (shadow * 0.9) + 0.9;\n" +
                "\t\t}\n" +
                "    gl_FragColor = (vColor * (diffuse + ambient + specular) * shadow);\n" +
                "}  ";
        mSimpleShadowProgram = new ShadersWork(depth_tex_v_with_shadow_shader, depth_tex_f_with_simple_shadow_shader);
        mActiveProgram = mSimpleShadowProgram.getProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)//изменяем положение объектов
    {
        mDisplayWidth = width;
        mDisplayHeight = height;
        GLES20.glViewport(0, 0, mDisplayWidth, mDisplayHeight);

        float ratio = (float) mDisplayWidth / mDisplayHeight;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 100.0f);
        Matrix.frustumM(mLightProjectionMatrix, 0, -1.1f * ratio, 1.1f * ratio, 1.1f * -1.0f, 1.1f * 1.0f, 1.0f, 100.0f);
    }

    @Override
    public void onDrawFrame(GL10 unused)//рисуем на экран
    {
        //получаем дескриптор программы
        mActiveProgram = mSimpleShadowProgram.getProgram();

        //устанавливаем камеру в позицию просмотра
        Matrix.setLookAtM(mViewMatrix, 0,
                3, 5, 0,
                0, 0, 0,
                -1,0,0);

        /*получаем расположение переменных в программе*/
        scene_mvpMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "uMVPMatrix");
        scene_mvMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "uMVMatrix");
        scene_normalMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "uNormalMatrix");
        scene_lightPosUniform = GLES20.glGetUniformLocation(mActiveProgram, "uLightPos");
        scene_shadowProjMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "uShadowProjMatrix");
        scene_positionAttribute = GLES20.glGetAttribLocation(mActiveProgram, "aPosition");
        scene_normalAttribute = GLES20.glGetAttribLocation(mActiveProgram, "aNormal");
        scene_colorAttribute = GLES20.glGetAttribLocation(mActiveProgram, "aColor");

        float[] basicMatrix = new float[16];

        /*делаем единичные матрицы*/
        Matrix.setIdentityM(basicMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        /*перемножаем единичную матрицу с вектором текущей позиции света*/
        Matrix.multiplyMV(mActualLightPosition, 0, basicMatrix, 0, mLightPosModel, 0);

        /*убираем задние грани объектов*/
        GLES20.glCullFace(GLES20.GL_BACK);
        /*рисуем сами объекты*/
        draw_picture();
    }

    private void draw_picture()//отрисовываем объекты
    {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);//очищаем буфер

        GLES20.glUseProgram(mActiveProgram);//используем нашу программу

        GLES20.glViewport(0, 0, mDisplayWidth, mDisplayHeight);//устанавливаем точку просмотра

        float[] tempResultMatrix = new float[16];//буферная матрица

        float[] bias = new float[]//матрица смещения
                {
                    0.5f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.5f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.5f, 0.0f,
                    0.5f, 0.5f, 0.5f, 1.0f
                };

        float[] depthBiasMVP = new float[16];//матрица глубины

        Matrix.multiplyMM(tempResultMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);//перемножаем матрицы моделей и отображения
        System.arraycopy(tempResultMatrix, 0, mMVMatrix, 0, 16);//копируем результат в общую матрицу

        GLES20.glUniformMatrix4fv(scene_mvMatrixUniform, 1, false, mMVMatrix, 0);//устанавливем значение переменной для матрицы

        Matrix.invertM(tempResultMatrix, 0, mMVMatrix, 0);//инвертируем общую матрицу и схораняем в буфер
        Matrix.transposeM(mNormalMatrix, 0, tempResultMatrix, 0);//затем транспонируем буфер и сохраняем в матрице нормалей

        GLES20.glUniformMatrix4fv(scene_normalMatrixUniform, 1, false, mNormalMatrix, 0);//устанавливем значение переменной для матрицы нормалей

        Matrix.multiplyMM(tempResultMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);//перемножаем матрицы проекции и перемноженные матрицы моделей и отображения
        System.arraycopy(tempResultMatrix, 0, mMVPMatrix, 0, 16);//копируем результат в БОЛЕЕ общую матрицу

        GLES20.glUniformMatrix4fv(scene_mvpMatrixUniform, 1, false, mMVPMatrix, 0);//устанавливем значение переменной для MVP матрицы

        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mActualLightPosition, 0);//перемножаем матрицу проекции и вектор света

        GLES20.glUniform3f(scene_lightPosUniform, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);//устанавливаем занчение для вектора

        Matrix.multiplyMM(depthBiasMVP, 0, bias, 0, mLightMvpMatrix, 0);//перемножаем смещение и матрицу света
        System.arraycopy(depthBiasMVP, 0, mLightMvpMatrix, 0, 16);//сохраняем результат в матрице глубины

        GLES20.glUniformMatrix4fv(scene_shadowProjMatrixUniform, 1, false, mLightMvpMatrix, 0);//устанавливаем переменную

        /*рисование объектов*/
        Cup.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
        Apple.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
        Table.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
        Beets.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
        Orange.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
        Cabbage.render(scene_positionAttribute, scene_normalAttribute, scene_colorAttribute);
    }
}

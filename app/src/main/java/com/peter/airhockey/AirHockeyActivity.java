package com.peter.airhockey;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.widget.Toast;

import com.peter.airhockey.util.LoggerConfig;
import com.peter.airhockey.util.MatrixHelper;
import com.peter.airhockey.util.SharderHelper;
import com.peter.airhockey.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class AirHockeyActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    private boolean renderderSet = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);

        if(checkSupportES2()) {
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setRenderer(new AirHockeyRenderer(this));
            renderderSet = true;
        } else {
            Toast.makeText(this,"The Device don't support OpenGL ES 2.0",Toast.LENGTH_LONG).show();
            return ;
        }

        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(renderderSet) {
            mGLSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(renderderSet) {
            mGLSurfaceView.onResume();
        }
    }

    /**
     * check if support OpenGL ES 2.0
     * @return
     */
    private boolean checkSupportES2() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    private class AirHockeyRenderer implements GLSurfaceView.Renderer{

        private final Context context;

        private static final int POSITION_COMPONMENT_COUNT = 2;

        private static final int BYTES_PER_FLOAT = 4;

        private final FloatBuffer vertexData;

        //顶点着色器名字
        private static final String A_COLOR = "a_Color";

        private static final String A_POSITION = "a_Position";

        private static final String U_MATRIX = "u_Matrix";

        //保存着色器中属性的位置
        private int aColorLocation;

        private int aPositionLocation;

        private int uMatrixLocation;

        private static final int COLOR_COMPONMENT_COUNT = 3;

        private static final int STRIDE = (POSITION_COMPONMENT_COUNT + COLOR_COMPONMENT_COUNT) * BYTES_PER_FLOAT;

        //顶点数组保存那个矩阵
        private final float[] projectionMatrix = new float[16];

        private final float[] modelMatrix = new float[16];

        float[] tableVerticesWithTriangles = {
                //Triangle FAN
                0f, 0f, 1f, 1f, 1f,
                -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
                0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
                0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,

                //Line 1
                -0.5f, 0f, 1f, 0f, 0f,
                0.5f, 0f, 1f, 0f, 0f,
                //Mallets
                0f, -0.4f, 0f, 0f, 1f,
                0f, 0.4f, 1f, 0f, 0f,
        };

        private int program;

        private AirHockeyRenderer(Context context) {
            this.context = context;
            //在本地内存创建一个缓冲区，并且将顶点位置数组复制到缓冲区
            vertexData = ByteBuffer
                    .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVerticesWithTriangles);

        }


        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            //1. 从资源文件中读取着色器
            String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_vertex_sharder);
            String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_fragment_sharder);
            //2. 编译着色器
            int vertexShader = SharderHelper.compileVertexSharder(vertexShaderSource);
            int fragmentShader = SharderHelper.compileFragmentSharder(fragmentShaderSource);
            //3. 把着色器一起链接进OpenGL程序
            program = SharderHelper.linkProgram(vertexShader,fragmentShader);
            //4. 验证程序
            if(LoggerConfig.ON) {
                SharderHelper.validateProgram(program);
            }
            //5.使用程序
            glUseProgram(program);
            //6.获取attribute color和position的位置
            aColorLocation = glGetAttribLocation(program, A_COLOR);

            aPositionLocation = glGetAttribLocation(program, A_POSITION);

            uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

            //每个缓冲区都有它内部的指针，设置起始位置
            vertexData.position(0);

            //6.告诉OpenGL，它可以在缓冲区找到对应a_Position位置
            glVertexAttribPointer(aPositionLocation, POSITION_COMPONMENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);

            //7.使能顶点
            glEnableVertexAttribArray(aPositionLocation);

            vertexData.position(POSITION_COMPONMENT_COUNT);

            glVertexAttribPointer(aColorLocation, COLOR_COMPONMENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);

            //使能颜色
            glEnableVertexAttribArray(aColorLocation);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            glViewport(0, 0, width, height);

            //创建正交矩阵
            /*final float aspectRatio = width > height ? (float)width / (float)height : (float)height / (float)width;

            if (width > height) Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
            else Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);*/

            //以45度视野创建透视投影矩阵,这个视锥体从Z值-1开始,-10结束
            MatrixHelper.perspectiveM(projectionMatrix, 45, (float)width / (float)height, 1f, 10f);

            //不要硬编码Z值,通过平移矩阵把桌子移出来,构建模型矩阵
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 0f,0f, -2.5f);
            Matrix.rotateM(modelMatrix, 0, -60f, 1f, 0f, 0f);

            //用临时矩阵保存,再copy到投影矩阵
            final float[] temp = new float[16];
            Matrix.multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0);
            System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {

            glClear(GL_COLOR_BUFFER_BIT);
            //
            glUniformMatrix4fv(uMatrixLocation,1,false, projectionMatrix,0);

            //前6个点绘制三角形
            glDrawArrays(GL_TRIANGLE_FAN,0,6);

            //接着2个点绘制直线
            glDrawArrays(GL_LINES, 6, 2);

            //绘制2个颜色不同的点
            glDrawArrays(GL_POINTS, 8, 1);
            glDrawArrays(GL_POINTS, 9, 1);
        }
    }
}
package com.luowei.surfacetexuredraw

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10
import android.content.ContentValues.TAG
import android.opengl.EGL14.eglSwapBuffers
import android.opengl.GLES20
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.opengl.EGL14.eglMakeCurrent




class MainActivity : AppCompatActivity() {


    inner class CameraV1GLRenderer : SurfaceTexture.OnFrameAvailableListener {
         private lateinit var mHandler: Handler

         private lateinit var mTextureView: TextureView

         private var mOESTextureId: Int=0

         private lateinit var mHandlerThread: HandlerThread

         private val MSG_INIT: Int = 1

         private val MSG_RENDER: Int=2

         //此init方法应该在Activity的OnCreate方法中调用
         fun init(textureView: TextureView, oesTextureId: Int) {
             mTextureView = textureView
             mOESTextureId = oesTextureId
             //开启子线程
             mHandlerThread = HandlerThread("Renderer Thread")
             mHandlerThread.start()
             //主线程与子线程需要通过Handler进行通信
             mHandler = object : Handler(mHandlerThread.getLooper()) {
                 override fun handleMessage(msg: Message) {
                     when (msg.what) {
                         MSG_INIT -> {
                             initEGL()
                             return
                         }
                         MSG_RENDER -> {
                             drawFrame()
                             return
                         }

                         else -> return
                     }
                 }
             }
             //初始化EGL环境
             mHandler.sendEmptyMessage(MSG_INIT)
         }

        private val transformMatrix= FloatArray(16)

        private fun drawFrame() {
             val t1: Long
             val t2: Long
             t1 = System.currentTimeMillis()
             if (mOESSurfaceTexture != null) {
                 mOESSurfaceTexture.updateTexImage()
                 mOESSurfaceTexture.getTransformMatrix(transformMatrix)
             }
             mEgl.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)
             GLES20.glViewport(0, 0, mTextureView.width, mTextureView.height)
             GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
             GLES20.glClearColor(1f, 1f, 0f, 0f)
//             mFilterEngine.drawTexture(transformMatrix)
             mEgl.eglSwapBuffers(mEGLDisplay, mEglSurface)
             t2 = System.currentTimeMillis()
//             Log.i(TAG, "drawFrame: time = " + (t2 - t1))
         }
         //定义所需变量
         private lateinit var mEgl: EGL10
         private var mEGLDisplay = EGL10.EGL_NO_DISPLAY
         private var mEGLContext = EGL10.EGL_NO_CONTEXT
         private val mEGLConfig = arrayOfNulls<EGLConfig>(1)
         private var mEglSurface: EGLSurface? = null

         private fun initEGL() {
             //获取系统的EGL对象
             mEgl = EGLContext.getEGL() as EGL10

             //获取显示设备
             mEGLDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
             if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
                 throw RuntimeException("eglGetDisplay failed! " + mEgl!!.eglGetError())
             }

             //version中存放当前的EGL版本号，版本号即为version[0].version[1]，如1.0
             val version = IntArray(2)

             //初始化EGL
             if (!mEgl!!.eglInitialize(mEGLDisplay, version)) {
                 throw RuntimeException("eglInitialize failed! " + mEgl!!.eglGetError())
             }

             //构造需要的配置列表
             val attributes = intArrayOf(
                     //颜色缓冲区所有颜色分量的位数
                     EGL10.EGL_BUFFER_SIZE, 32,
                     //颜色缓冲区R、G、B、A分量的位数
                     EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_NONE)
             val configsNum = IntArray(1)

             //EGL根据attributes选择最匹配的配置
             if (!mEgl!!.eglChooseConfig(mEGLDisplay, attributes, mEGLConfig, 1, configsNum)) {
                 throw RuntimeException("eglChooseConfig failed! " + mEgl!!.eglGetError())
             }

             //如本文开始所讲的，获取TextureView内置的SurfaceTexture作为EGL的绘图表面，也就是跟系统屏幕打交道
             val surfaceTexture = mTextureView.surfaceTexture ?: return

             //根据SurfaceTexture创建EGL绘图表面
             mEglSurface = mEgl!!.eglCreateWindowSurface(mEGLDisplay, mEGLConfig[0], surfaceTexture, null)

             //指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
             val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
             //创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
             mEGLContext = mEgl!!.eglCreateContext(mEGLDisplay, mEGLConfig[0], EGL10.EGL_NO_CONTEXT, contextAttribs)

             if (mEGLDisplay === EGL10.EGL_NO_DISPLAY || mEGLContext === EGL10.EGL_NO_CONTEXT) {
                 throw RuntimeException("eglCreateContext fail failed! " + mEgl!!.eglGetError())
             }

             //指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
             if (!mEgl!!.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)) {
                 throw RuntimeException("eglMakeCurrent failed! " + mEgl!!.eglGetError())
             }
         }
         override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
             if (mHandler != null) {
                 //因为没有GLSurfaceView的Renderer线程，所以我们需要自己通知自定义的Renderder子线程进行渲染图像
                 mHandler.sendEmptyMessage(MSG_RENDER);
             }
         }

     }



    private var mOESTextureId: Int=0

    private lateinit var mOESSurfaceTexture: SurfaceTexture

    private lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaPlayer = MediaPlayer()

        val fileDescriptor = assets.openFd("1.mp4")
        mediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.declaredLength)
        mediaPlayer.prepare()
        textureView = TextureView(this)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return true
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                //获取外部纹理ID
                mOESTextureId = createOESTextureObject();
                //获取自定义的SurfaceTexture
                mOESSurfaceTexture = initOESTexture();
                mediaPlayer.setSurface(Surface(mOESSurfaceTexture))
                mediaPlayer.start()
            }

        }
        setContentView(textureView)
    }


    //创建外部纹理
    fun createOESTextureObject(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }

    //根据外部纹理ID创建SurfaceTexture
    fun initOESTexture(): SurfaceTexture {
        mOESSurfaceTexture = SurfaceTexture(mOESTextureId)
        val cameraV1GLRenderer = CameraV1GLRenderer()
        cameraV1GLRenderer.init(textureView,mOESTextureId)
        mOESSurfaceTexture.setOnFrameAvailableListener(cameraV1GLRenderer)
        return mOESSurfaceTexture
    }

}

package com.example.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by KingChaos on 2017/4/26.
 */
//第一个传的是字符串，第二个为进度显示单位，第三个用于返回执行结果
public class DownloadTask extends AsyncTask<String, Integer,Integer> {
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;
    private DownloadListener listener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;
    public DownloadTask(DownloadListener listener){
        this.listener=listener;
    }
    @Override
    //...语法是Java5开始的，对方法参数支持的一种新写法，叫做可变参数列表，表示此处接收的参数为0到多个Object类型的对象，或者是一个Object[].
    protected Integer doInBackground(String... strings) {

        //在这里执行后台的具体下载逻辑
        InputStream is=null;
        RandomAccessFile savedFile=null;
        File file=null;
        try {
            long downloadedLength=0;
            String downloadUrl=strings[0];
            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //Environment.DIRECTORY_DOWNLOADS指定了下载到SD卡的download目录下
            String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file=new File(directory+fileName);
            if(file.exists()){
                //存在就读取已下载字节数，用于在后面启动断点续传
                downloadedLength=file.length();
            }
            long contentLength=getContentLength(downloadUrl);

            if(contentLength==0){
                //获取文件总长度，如果为0说明文件有问题
                return TYPE_FAILED;
            }else if(contentLength==downloadedLength){
                //已下载字节和文件总字节相等，说明已经下载完成
                return TYPE_SUCCESS;
            }
            OkHttpClient client=new OkHttpClient();
            Request request=new Request.Builder()
                    //断点下载，指定从哪个字节开始下载
                    //请求中加上Header告诉服务器从哪个字节开始下载
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .url(downloadUrl)
                    .build();

            Response response=client.newCall(request).execute();

            if(response!=null){
                is=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);//跳过已下载字节
                byte[] b=new byte[1024];
                int total=0;
                int len;
                while((len=is.read(b))!=-1){
                    if(isCanceled) {
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else{
                        total+=len;
                        savedFile.write(b,0,len);
                        //计算下载百分比
                        int progress = (int) ((total+downloadedLength)*100/contentLength);
                        //通知下载进度
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;



            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is!=null) {
                    is.close();
                }
                if (savedFile!=null) {
                    savedFile.close();
                }
                if (file!=null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        //用于在界面上更新当前下载进度
        int progress=values[0];
        if (progress>lastProgress) {
            lastProgress=progress;
            listener.onProgress(progress);
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        //通知最终的下载结果
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;

        }
    }
    public void pauseDownload(){
        isPaused=true;
    }

    public void cancelDownload(){
        isCanceled=true;
    }
    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client =new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();

        Response response=client.newCall(request).execute();
        if (response!=null&&response.isSuccessful()) {
            long contentLength=response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }


}

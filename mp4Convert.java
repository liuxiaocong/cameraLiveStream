package main;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import main.GetProperty;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class mp4Convert {

	private static String logFileRoot = "D:\\javacv\\log\\";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GetProperty getProperty = new GetProperty();

		Properties prop = null;
		try {
			prop = getProperty.getPropValues();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int settingLongSide = 640;
		int settingShortSide = 360;
		
		String recorderPidUrl = "";

		if (prop != null) {

			System.out.println(prop.getProperty("width"));
			settingLongSide = Integer.parseInt(prop.getProperty("width"));
			settingShortSide = Integer.parseInt(prop.getProperty("height"));
			recorderPidUrl = prop.getProperty("purl");
			
			logFileRoot = prop.getProperty("logroot");

		}

		long timeRecord = 0;
		int sleepTime = 0;
		String source = "D:\\javacv\\a.mp4";

		//source = "https://r3---sn-htgx20capj-npol.googlevideo.com/videoplayback?dur=0.000&expire=1438098052&key=yt5&ip=101.100.176.125&lmt=1431968026770928&sver=3&id=o-ABjUG8yPTkClQQHVX19dqamlBKZ4_3poUdtdBBn3dQrG&source=youtube&initcwndbps=3967500&mm=31&mn=sn-htgx20capj-npol&ratebypass=yes&requiressl=yes&ms=au&mt=1438076332&mv=m&pl=27&itag=43&mime=video%2Fwebm&fexp=901816%2C906335%2C9406990%2C9408710%2C9415365%2C9415485%2C9416126&pcm2cms=yes&ipbits=0&sparams=dur%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpcm2cms%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cupn%2Cexpire&signature=8A99A9B3FB218BC91CF2914211EEF819FC27FB7B.AF9799F7CAA64CBD059E6280F501E361EE968C4E&upn=FH6wi19X78Y";

		//source = "https://manifest.googlevideo.com/api/manifest/hls_playlist/id/R_LXS1Af6SI.1/itag/92/source/yt_live_broadcast/requiressl/yes/ratebypass/yes/live/1/cmbypass/yes/gir/yes/dg_shard/Ul9MWFMxQWY2U0kuMQ.92/hls_chunk_host/r1---sn-htgx20capj-npol.googlevideo.com/pmbypass/yes/playlist_type/LIVE/gcr/sg/mm/32/mn/sn-htgx20capj-npol/ms/lv/mv/u/pcm2cms/yes/pl/27/dover/3/upn/eKuclrn-Ai0/sver/3/fexp/901816,9407641,9408710,9409246,9410705,9414661,9414806,9415356,9415365,9415436,9415485,9415956,9416126,9416358,9416656,9417949,9418008/keepalive/yes/mt/1438076602/ip/101.100.176.125/ipbits/0/expire/1438098633/sparams/ip,ipbits,expire,id,itag,source,requiressl,ratebypass,live,cmbypass,gir,dg_shard,hls_chunk_host,pmbypass,playlist_type,gcr,mm,mn,ms,mv,pcm2cms,pl/signature/0BC788B3E761CD80E120405CCD6082BFF910AB19.7CEC39E6472FE00396F99EC6EF1ECC0B12AD9069/key/dg_yt0/playlist/index.m3u8";
		
		//source = "http://d2bi7sjjwydkz8.cloudfront.net:80/vod/mp4:10001111_1437561826790.mp4/playlist.m3u8";
		
		String url = "rtmp://52.74.32.141:1935/live/10006872_2082516_776112_3";
		String isAutoLoop = "false";

		Long lastTimeStamp = 0L;
		Long lastTime = 0L;

		// second
		Long expectTime = 21600L;

		int scheduleId = 0;
		
		if (args.length >= 5) {

			source = args[0];
			url = args[1];
			isAutoLoop = args[2];
			if(Long.parseLong(args[3]) > 0)
			{
				expectTime = Long.parseLong(args[3]);
			}
			
			scheduleId = Integer.parseInt(args[4]);

		}
		
		if (args.length >= 4) {

			source = args[0];
			url = args[1];
			isAutoLoop = args[2];
			if(Long.parseLong(args[3]) > 0)
			{
				expectTime = Long.parseLong(args[3]);
			}

		}

		if (args.length >= 3) {

			source = args[0];
			url = args[1];
			isAutoLoop = args[2];

		} else if (args.length >= 2) {

			source = args[0];
			url = args[1];

		} else if (args.length == 1) {
			source = args[0];
		}

		System.out.println(isAutoLoop);

		System.out.println("mp4Convert");

		System.out.println("source: " + source);

		System.out.println("url: " + url);

		System.out.println("isAutoLoop: " + isAutoLoop);

		System.out.println("expectTime: " + expectTime);
		
		System.out.println("recorderPidUrl: " + recorderPidUrl);

		final Logger logger = getLogger();

		try {

			RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
			String name = runtime.getName();
			System.out.println("TAG:" + name);
			int index = name.indexOf("@");
			int pid = 0;
			if (index != -1) {
				pid = Integer.parseInt(name.substring(0, index));
				System.out.println("PID:" + pid);
			}
			
			recorderPid(recorderPidUrl,scheduleId,pid);
			
			FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(source);

			frameGrabber.start();

			int frameWidth = frameGrabber.getImageWidth();
			int frameHeight = frameGrabber.getImageHeight();

			System.out.println("frameWidth: " + frameWidth);
			System.out.println("frameHeight: " + frameHeight);

			int outPutWidth = frameWidth;
			int outPutHeight = frameHeight;

			double scale = 1;

			if (frameWidth > frameHeight) {

				if (frameWidth > settingLongSide) {

					scale = (double) settingLongSide / (double) frameWidth;

				}

			} else {

				if (frameHeight > settingLongSide) {

					scale = (double) settingLongSide / (double) frameHeight;

				}
			}

			outPutWidth = (int) (outPutWidth * scale);
			outPutHeight = (int) (outPutHeight * scale);

			if (outPutWidth % 2 == 1) {
				outPutWidth = outPutWidth - 1;
			}

			if (outPutHeight % 2 == 1) {
				outPutHeight = outPutHeight - 1;
			}

			// Log.d("bharat:", "frame count " +
			// frameGrabber.getLengthInFrames() + "");

			System.out.println("outPutWidth: " + outPutWidth);
			System.out.println("outPutHeight: " + outPutHeight);

			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(url,
					outPutWidth, outPutHeight, 1);

			recorder.setVideoCodec(org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264);
			recorder.setAudioCodec(org.bytedeco.javacpp.avcodec.AV_CODEC_ID_AAC);
//			recorder.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P);
			recorder.setFormat("flv");			
			
			recorder.setGopSize((int) frameGrabber.getFrameRate() * 4);
			recorder.setTimestamp(0);

			// necessory becoz of audio..
			System.out.println(" frame rate" + frameGrabber.getFrameRate());
			System.out.println(" Sample Rate" + frameGrabber.getSampleRate());
			System.out.println(" Sample Format"
					+ frameGrabber.getSampleFormat());
			// System.out.println(" Sample Rate"+ );

			recorder.setFrameRate(frameGrabber.getFrameRate());

			// fuck ?
			// recorder.setSampleFormat(frameGrabber.getSampleFormat());
			recorder.setSampleFormat(1);
			recorder.setSampleRate(frameGrabber.getSampleRate());

			recorder.setAudioBitrate(16000);

			if (logger != null) {

				logger.info("isAutoLoop: " + isAutoLoop);
				logger.info("Source: " + source);
				logger.info("rtmp url: " + url);
				logger.info("Frame rate: " + frameGrabber.getFrameRate());
				logger.info("Sample Rate: " + frameGrabber.getSampleRate());
				logger.info("Width: " + outPutWidth);
				logger.info("Height: " + outPutHeight);
				logger.info("expectTime: " + expectTime);
				logger.info("pid: " + pid);

			}

			recorder.start();

			long currentTimeMillis = System.currentTimeMillis();

			int videoCount = 0;
			int totalcount = 0;
			int soundCount = 0;

			long now = System.currentTimeMillis();

			long targetTime = 0;

			while (true) {
				try {

					timeRecord = System.currentTimeMillis();

					Frame grabFrame = frameGrabber.grabFrame();

					if (grabFrame == null) {

						System.out.println("timeRecord" + timeRecord);

						if (isAutoLoop.equals("true")) {

							if (((timeRecord - now) / 1000) > expectTime) {
								System.out.println("End");
								recorder.stop();
								recorder.release();
								System.exit(-1);
								break;

							} else {

								Thread.sleep(10000);

								recorder.stop();

								frameGrabber.releaseUnsafe();

								frameGrabber = new FFmpegFrameGrabber(source);

								frameGrabber.start();

								now = System.currentTimeMillis();

								recorder.setTimestamp(0);

								recorder.start();

								System.out.println("restart");

								// grabFrame = frameGrabber.grabFrame();
							}

						} else {

							System.out.println("End");
							break;
						}
					}
					totalcount++;

					if (grabFrame != null) {

						if (grabFrame.image != null) {	
							
//							byte[] imagebype = new byte[grabFrame.image.length];
//							
//							for(int i = 0; i < grabFrame.image.length;i++){
//								imagebype[i] = ((ByteBuffer) grabFrame.image[0]).get(i);
//							}
//							
//							org.bytedeco.javacpp.opencv_core.IplImage rgbImageTemp = org.bytedeco.javacpp.opencv_core.IplImage.create(400, 270, IPL_DEPTH_8U, 3);
//							
//							rgbImageTemp.getByteBuffer().put(imagebype);
//							
//							org.bytedeco.javacpp.opencv_core.IplImage rgbImage = org.bytedeco.javacpp.opencv_core.IplImage.create(400, 270, IPL_DEPTH_8U, 3);
//							
//							org.bytedeco.javacpp.opencv_imgproc.cvCvtColor(rgbImageTemp, rgbImage, org.bytedeco.javacpp.opencv_imgproc.CV_YUV2BGR_NV21);
//							
//							
//							org.bytedeco.javacpp.opencv_highgui.cvSaveImage("a" + totalcount + ".jpg", rgbImageTemp);

							
						
							//File targetFile = new File("aa" + totalcount + ".jpg");  
							
							//ImageIO.write(bImage, "jpg", targetFile);  
							
							videoCount++;
						}
						if (grabFrame.samples == null) {
							soundCount++;
						}
						// Log.d("bharat:", "sound data :" + sound);

						if (recorder.getTimestamp() < frameGrabber
								.getTimestamp()) {
							recorder.setTimestamp(frameGrabber.getTimestamp());
						}

						if ((timeRecord - now) < (frameGrabber.getTimestamp() / 1000)) {

							if (source.startsWith("http")) {
								
								sleepTime = (int) (((float) 1 / (float) (frameGrabber
										.getFrameRate())) * 1000);
							} else {

								sleepTime = (int) ((frameGrabber.getTimestamp() / 1000) - (timeRecord - now));

							}
							
							//System.out.println("sleepTime:" + sleepTime);
							
							Thread.sleep(sleepTime);
						}

						if (frameGrabber.getTimestamp() == 0L) {

							lastTime = lastTime + lastTimeStamp / 1000000;

						}

						lastTimeStamp = frameGrabber.getTimestamp();

						// System.out.println("lastTimeStamp: " +
						// lastTimeStamp);
						//
						// System.out.println("cc: " + (lastTime +
						// (lastTimeStamp / 1000000)));

						if ((timeRecord - now) / 1000 > expectTime) {

							System.out.println("time Record: " + timeRecord);

							System.out.println("start time: " + now);

							System.out
									.println("force stop time: " + expectTime);

							recorder.stop();
							recorder.release();
							System.exit(-1);
							break;

						}

						// System.out.println("frameGrabber.getTimestamp(): "
						// + frameGrabber.getTimestamp() / 1000);
						//
						// System.out.println("targetTime: " + targetTime);
						//
						// if (frameGrabber.getTimestamp() / 1000 >= targetTime)
						// {
						//
						// System.out.println("Recorded");
						// recorder.record(grabFrame);
						// targetTime = targetTime + 50;
						//
						// }

						// if(grabFrame.samples!=null)
						// {
						// recorder.recordSamples(grabFrame.samples);
						// }
						//
						// if (grabFrame.image != null) {
						// recorder.recordImage(grabFrame.image);
						// }

						recorder.record(grabFrame);

						//
						// System.out.println("self timestamp:" + (long)
						// (System.currentTimeMillis() - now));
						//
						// System.out.println("1000 / frameGrabber.getFrameRate(): "
						// + (int) ((1000 / frameGrabber.getFrameRate())));
						//
						// System.out.println("System.currentTimeMillis() - timeRecord - 1: "
						// + (long) (System.currentTimeMillis() - timeRecord -
						// 1));

						// System.out.println("Sleep time 1: " + sleepTime);
						// System.out.println("Sleep time: " + ((int) (1000 /
						// frameGrabber.getFrameRate()) - 30));
						//

						// if (sleepTime > 0) {
						//
						// Thread.sleep(sleepTime);
						// }
					}

				} catch (Exception e) {
					System.out.println("exception " + e);
					e.printStackTrace();
				}

			}
			System.out.println("frame done, sound count: " + soundCount
					+ " ,video count: " + videoCount + " ,total count"
					+ totalcount);

			if (logger != null) {

				logger.info("frame done, sound count: " + soundCount
						+ " ,video count: " + videoCount + " ,total count"
						+ totalcount);

			}
			recorder.stop();
			recorder.release();
			System.exit(-1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static void recorderPid(String url,int sid,int pid)
	{
		
		if(url=="")
		{
			return;
			
		}else
		{
			url = url + sid + "?pid=" + pid;
			URL urlObj;
	        HttpURLConnection conn = null;
	        String ret = "";
	        InputStream inputStream = null;
	        try {
	            urlObj = new URL(url);
	            conn = (HttpURLConnection) urlObj.openConnection();
	            conn.setConnectTimeout(6 * 1000);  //设置链接超时时间6s
	            conn.setRequestMethod("GET");
	            if (conn.getResponseCode() == 200) {
	                
	            }
	
	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (ProtocolException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            if (inputStream != null) {
	                try {
	                    inputStream.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	            if (conn != null) {
	                conn.disconnect();
	            }
	        }
		}
	}

	private static Logger getLogger() {
		Logger logger = Logger.getLogger("main");
		logger.setLevel(Level.ALL);
		FileHandler fileHandler = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String trace = dateFormat.format(new Date().getTime());
		String logFilePath = logFileRoot + "jty-log-" + trace + ".log";
		File file = new File(logFilePath);
		try {
			fileHandler = new FileHandler(logFilePath, true);

			fileHandler.setFormatter(new java.util.logging.Formatter() {
				@Override
				public String format(LogRecord arg0) {
					SimpleDateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");// set date
					return df.format(new Date()) + "\t" + arg0.getLoggerName()
							+ "\t" + arg0.getLevel().getName() + "\t"
							+ arg0.getMessage() + "\n\r\n";
				}
			});

			logger.addHandler(fileHandler);

		} catch (SecurityException | IOException e1) {
			// TODO Auto-generated catch block

			logger = null;
			e1.printStackTrace();
		}

		logger.info("finish getLogger");

		return logger;
	}
}

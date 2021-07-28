# MediaCodec-Recorder
The screen recorder for Android names as "MediaCodec"
# The classes
Worker - the writer thead
RecordService - Service for recording video from source
# Why i am not using MediaRecorder?
I am not using MediaRecorder beacuse it's requests a bitrate and need to calculate it,
In my worker it's is -1 because bitrate-mode CQ it means the mediacodec automatically set's the bitrate from surface data

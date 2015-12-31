# ImageCrop

ImageCrop是一个非常简单的图片裁剪工具，一共只有不超过1000行代码，如果你需要图片裁剪功能，你可以非常容易地读懂并改造这个工程。

使用ImageCrop时，你需要指定要裁剪的图片地址（目前必须是本地图片），裁剪后图片的输出地址，以及裁剪框的位置和大小，裁剪类型（要裁剪成方形还是圆形）。

选取裁剪区域时，裁剪框的位置和大小不会改变，你需要拖动图片来选择需要裁剪的区域。支持的交互有单指拖动、双指缩放、双击缩放。

裁剪时，直接读取原图进行裁剪，保证图片清晰。因此，裁出的图片总会比裁剪框更大 － 或是相等。

##for gradle:
    compile 'tech.gaolinfeng:SimpleAndroidImageCrop:0.1.1'

##usage:
要调起图片裁剪，首先传入基本的参数，构造一个intent，并启动裁剪Activity：

Intent intent = ImageCropActivity.createIntent(
  activity, inImagePath, outImagePath, "200, 200, 1000, 1000", cropCircle);
  
然后，你就可以在onActivityResult中拿到裁剪后的图片了，裁剪完毕图片会被存放在outImagePath中。

##截图:
<img src="https://github.com/cuc-glf/ImageCrop/blob/master/imgs/entry.png" alt="entry" />

<img src="https://github.com/cuc-glf/ImageCrop/blob/master/imgs/circle_crop.png" alt="circle crop" />

<img src="https://github.com/cuc-glf/ImageCrop/blob/master/imgs/rect_crop.png" alt="rect crop" />

<img src="https://github.com/cuc-glf/ImageCrop/blob/master/imgs/result.png" alt="result" />


have fun!

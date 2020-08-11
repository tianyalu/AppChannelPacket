

# `Android`多渠道包生成实践

[TOC]

## 一、理论

### 1.1 多渠道打包意义

渠道包需要包含不同的渠道信息，在`APP`和后台交互或者数据上报时，会带上各自的渠道信息。统计每个分发市场的下载数、用户数等关键数据。

### 1.2 多渠道打包方案

多渠道打包有以下几种方案：

> 1. 使用`ProductFlavor`进行多渠道打包，每个渠道包都需要进行一次完整的打包流程，速度慢；
> 2. `META_INF`目录添加渠道文件；
> 3. `APK`文件末尾追加渠道注释；
> 4. 针对`Android7.0`新增的`V2`签名方案的`APK`添加渠道`ID-value`。

### 1.3  `apk`打包流程

 `apk`打包流程如下图所示：

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/apk_pack_process.png)

### 1.4 `apk`签名

`apk`的签名信息是保存在`META-INF`目录下的，该目录下主要有`MANIFEST.MF`、`CERT.SF`和`CERT.RSA`三个文件，其作用如下所示：

> 1. `MANIFEST.MF`：是摘要文件，保存了`apk`所有文件的摘要信息，其中摘要信息是经过**`SHA1`**生成摘要信息，然后再进行**`Base64`**编码所得；
> 2. `CERT.SF`：是对摘要的签名文件，保存了对`MANIFEST.MF`文件再进行一次**`SHA1`**并且**`Base64`**加密的信息，并且同时保存了`MANIFEST.MF`文件的摘要信息；
> 3. `CERT.RSA`：保存了公钥和所采用的加密算法等信息，此外最重要的还包含了对`CERT.SF`文件内容的摘要用私钥加密后的值。

**说明：** 第三步才是最重要的，在这一步，即使开发者修改了程序内容，并生成了新的摘要文件，但是攻击者没有开发者的私钥，所以不能生成正确的签名文件（`CERT.SF`）。系统在对程序进行验证的时候，用开发者公钥对不正确的签名文件进行解密，得到的结果和摘要文件（`MANIFEST.MF`）对应不起来，所以不能通过检验，不能成功安装文件。

参考：[安卓签名机制](https://www.52pojie.cn/thread-304135-1-1.html)

[Android签名与认证原理](http://www.itkeyword.com/doc/4503082654655949x675/CERT.SF-CERT.RSA-MANIFEST.MF)

**疑问：**

这里有一个小疑问，百思不得其解：第二步没有任何使用私钥加密的步骤，如果第三者在第三步使用他自己的私钥加密，并将其公钥放到`CERT.RSA`文件中，岂不是能起到偷梁换柱的作用？

#### 1.4.1 `V1`签名

V1 签名：保护**现有**的文件。但是校验时不会对`META-INF`目录下的文件进行校验，可以利用这一特性，在`apk`的`META-INF`目录下新建一个包含渠道名称或id的空文件，`apk`启动时，读取该文件来获取渠道号，从而达到区分各个渠道包的作用。

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/apk_file_content.png)

#### 1.4.2 `V2/V3`签名

`V2`签名也称`Full APK signature`，它是一个对全文件进行签名的方案，能提供更快的应用安装时间、对未授权`APK`文件的更改提供保护。

> * `V2`签名：`Android 7.0`及更高版本设备支持；
> * `V3`签名：`Android 9.0`及更高版本设备支持。

该方案会对`APK`的内容进行哈希处理和签名，然后按照zip文件格式将生成的“`APK`签名块”插入到`APK`中，如下图所示：

参考：[https://source.android.google.cn/security/apksigning](https://source.android.google.cn/security/apksigning)

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/v2_v3_signing_structure.png)

签名分块数据如下图所示：

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/v2_signing_block.png)

我们注意到`ID-value`，它是由一个 8字节的长度标识 + 4字节的`ID` + 它的负载 组成。`V2`的签名信息时以固定的`ID`值（`0x7109871a`）的`ID-value`来保存在这个区块中，也就是说它可以有若干个这样的`ID-value`来组成：

| `Length` | `ID`         | `Data`       |
| -------- | ------------ | ------------ |
| ...      | ...          | ...          |
| 签名长度 | `0x7109871a` | 安卓签名信息 |
| ...      | ...          | ...          |

受保护的内容：

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/v2_signing_protected_area.png)

另外签名校验时会忽略除了`Android`签名信息外的其它`ID-value`的，所以可以把渠道信息添加到`ID-value`中，实现生成多渠道包了。

另外需要注意的是`APK Signing Block`是使用小端模式来保存字节的，我们使用的时候也必须使用小端模式：

> 小端模式：数据的高字节保存在内存的高地址中，二数据的低字节保存在内存的低地址中。这种存储模式将地址的高低和数据位权有效地结合起来，高地址部分权值高，低地址权值低。
>
> 也就是说，如`0x1234`用小端模式保存的话就是：
> byte[0] = 0x34 --> 低字节保存在低地址；
> byte[1] = 0x12 --> 高字节保存在高地址。


#### 1.4.3 `V1`和`V2/V3`区别

`apk`签名`V1`和`V2/V3`区别如下表所示：

| 签名方案    | 兼容版本                                   | 保护对象                              |
| ----------- | ------------------------------------------ | ------------------------------------- |
| `V1`签名    | 基于`JAR`签名，全版本                      | 保护`zip`中的文件                     |
| `V2/V3`签名 | `Android 7.0 V2`方案，`Android 9.0 V3`方案 | 保护整个`apk`(除特殊区域外)的字节数据 |

#### 1.4.4 签名验证过程

系统安装`APK`时签名验证过程如下图所示：

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/signature_validate_sequence.png)

验证程序会对照存储在“`APK`签名分块”中的`V2+`签名对`APK`的全文件哈希进行验证。该哈希涵盖除“`APK`签名分块”（其中包含`V2+`签名）之外的所有内容。在“`APK`签名分块”以外对`APK`进行的修改都会使`APK`的`V2+`签名作废。`V2+`签名被删除的`APK`也会被拒绝，因为`V1`签名指明相应`APK`带有`V2`签名，所以`Android 7.0`及更高版本会拒绝使用`V1`签名验证`APK`。

### 1.5 `V2`签名添加渠道`ID-value`

#### 1.5.1 实现步骤

> 1. 解析`APK`，判断是否使用`V2/V3`签名，定位`V2/V3`签名块；
> 2. 在签名块中添加包含渠道信息的`ID-Value`；
> 3. 拷贝原`APK`，并修改签名块数据生成带有渠道信息的`APK`。



![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/v2_signing_blockdata.png)



#### 1.5.2 方案落地

问题：

> 1. 怎么判断是否使用`V2`签名？
> 2. 如何定位签名块？签名块从第几个字节开始？

`EOCD`格式（`End of Central Directory`）中的偏移量16描述了核心目录（即第三块`Central Directory`）相对于整个`zip`压缩文件的偏移量`offset`，下图中右侧内容表示第二块`APK Signing Block`，则从`offset`向左读取16个字节便得到了魔数`magic`（可以通过 [`010Editor`]( https://www.sweetscape.com/download/010editor/) 查看文件的数据格式`magic`），然后判断这个`magic`和 [ApkSigningBlockUtils.java](https://www.androidos.net.cn/android/9.0.0_r8/xref//tools/apksig/src/main/java/com/android/apksig/internal/apk/ApkSigningBlockUtils.java) 中的 `APK_SIGNING_BLOCK_BYTES` 的16个字节是否相同，如果相同，则表明使用了`V2/V3`签名。

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/eocd.png)

从`magic`的位置再向左读取8个字节可以得到`size of block`，即第二块`APK Signing Block`的总长度，此时可以将第二块的数据抽离出来，然后按照原来的格式添加自己的渠道`ID-Value`信息。

**生成渠道`APK`步骤：**

> 1. 第一部分无需修改，直接从原`APK`读取并写出到新的`APK`中；
> 2. 将添加了包含渠道信息`ID-Value`的签名块写入到新`APK`；
> 3. 第三部分同样无需修改；
> 4. 第四部分修改第16个字节开始后四个字节再写入`APK`(因为插入了渠道信息，导致核心目录偏移量改变了)。

**注：** 第四部分修改的内容不会影响签名校验。

### 1.6 题外话

#### 1.6.1 资源混淆

资源混淆如何实现？

![image](https://github.com/tianyalu/AppChannelPacket/raw/master/show/mix_resource_file.png)

#### 1.6.2 高性能日志设计

[微信终端跨平台组件 mars 系列（一） - 高性能日志模块xlog](https://mp.weixin.qq.com/s/cnhuEodJGIbdodh0IxNeXQ?)

## 二、实践

### 2.1 使用`ProductFlavor`进行多渠道打包

使用`Flavor`方式生成渠道包的优点原生的方式，而且灵活；但是缺点是一旦生成的渠道包多的话所需要的时间就会成倍地增加，效率不高。

#### 2.1.1 `AndroidManifest.xml`

`application`标签下添加：

```xml
<meta-data android:name="CHANNEL_VALUE" android:value="${channel1}"/>
```

#### 2.1.2 创建渠道文件

在`app/build.gradle`同级目录新建渠道文件`channel.txt`:

```tex
xiaomi
baidu
360
huawei
```

#### 2.1.3 `build.gradle`文件

`app/build.gradle`文件`android`节点下添加如下脚本：

```groovy
flavorDimensions "default"
//读取channel.txt 渠道文件，创建flavor
file('channel.txt').readLines().each{
  channel ->
  productFlavors.create(channel, {
    dimension "default"
    //替换AndroidManifest中的值
    manifestPlaceholders = [channel1: channel]
  })
}
```

#### 2.1.4 获取渠道信息

```java
//读取Manifest中的 meta-data 渠道信息
private void getChannel() {
  try {
    PackageManager pm = getPackageManager();
    ApplicationInfo appInfo = pm.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
    String channel = appInfo.metaData.getString("CHANNEL_VALUE");
    Toast.makeText(this, "当前渠道：" + channel, Toast.LENGTH_SHORT).show();
  } catch (PackageManager.NameNotFoundException e) {
    e.printStackTrace();
  }
}
```
### 2.2 `META_INF`目录添加渠道文件

在`APK`的`META-INF`目录下新建一个包含渠道名称或`ID`的文件，`APK`启动时，读取该文件来获取渠道号，从而达到区分各个渠道的作用。

#### 2.2.1 添加渠道文件

因为`APK`实际是`zip`文件，对于`Java`来说，使用`ZipFile`、`ZipEntry`、`ZipOutputStream`等类很简单就能操作`zip`文件：

```java
private static final String META_INF_PATH = "META-INF" + File.separator;
private static final String CHANNEL_PREFIX = "channel_";
private static final String CHANNEL_PATH = META_INF_PATH + CHANNEL_PREFIX;

public static void addChannelFile(ZipOutputStream zos, String channel, String channelId) throws IOException {
  // Add Channel file to META-INF
  ZipEntry emptyChannelFile = new ZipEntry(CHANNEL_PATH + channel + "_" + channelId);
  zos.putNextEntry(emptyChannelFile);
  zos.close();
}
```

#### 2.2.2 读取渠道文件

只需要遍历`apk`文件，找到我们添加的渠道文件即可：

```java
/**
 * 从渠道 apk 文件中读取 META-INF 中的渠道信息
 * @param apkFile
 * @return
 */
public static String getChannelByMetaInf(File apkFile) {
  String channel = "";
  if(apkFile == null || !apkFile.exists()) {
    return channel;
  }

  try {
    ZipFile zipFile = new ZipFile(apkFile);
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String name = entry.getName();
      if(name == null || name.trim().length() == 0 || !name.startsWith(META_INF_PATH)) {
        continue;
      }
      name = name.replace(META_INF_PATH, "");
      if(name.startsWith(CHANNEL_PREFIX)) {
        channel = name.replace(CHANNEL_PREFIX, "");
        break;
      }
    }
  } catch (IOException e) {
    e.printStackTrace();
  }
  return channel;
}
```

或者有童鞋会问，读渠道文件是程序在跑时读的，我们手机如何拿到`Apk`文件，总不能要用户手机都保留一个Apk文件吧？如果有这疑问的童鞋，可能不知道手机上安装的应用都会保留应用的`Apk`的，并且安卓也提供了`Api`，只需简单几行代码就能获取，可以参考本项目的代码。

#### 2.2.3 生成多个渠道包

通过`Java`代码编写一个脚本，根据渠道配置文件，读取所需的渠道，再复制多个原`Apk`文件作为渠道包，最后往渠道包中添加渠道文件就可以了：

```java
/**
 * 添加渠道信息到 apk 文件中（创建新的渠道apk文件）
 * @param apkFile
 */
public static void addChannelToApk(ZipFile apkFile) {
  if(apkFile == null) {
    throw new NullPointerException("Apk file can not be null");
  }

  Map<String, String> channels = getAllChannels();
  Set<String> channelSet = channels.keySet();
  String srcApkName = apkFile.getName().replace(".apk", "");
  srcApkName = srcApkName.substring(srcApkName.lastIndexOf(File.separator) + 1);

  for (String channel : channelSet) {
    String channelId = channels.get(channel);
    ZipOutputStream zos = null;
    try {
      File channelFile = new File(BUILD_DIR, srcApkName + "_" + channel + "_" + channelId + ".apk");
      if(channelFile.exists()) {
        channelFile.delete();
      }
      FileUtils.createNewFile(channelFile);
      zos = new ZipOutputStream(new FileOutputStream(channelFile));
      copyApkFile(apkFile, zos);

      MetaInfProcessor.addChannelFile(zos, channel, channelId);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(zos);
    }

  }
  IOUtils.closeQuietly(apkFile);
}

/**
 * 将源 apk 文件完全复制到新的输出流中
 * @param src
 * @param zos
 * @throws IOException
 */
private static void copyApkFile(ZipFile src, ZipOutputStream zos) throws IOException {
  Enumeration<? extends ZipEntry> entries = src.entries();
  while (entries.hasMoreElements()) {
    ZipEntry zipEntry = entries.nextElement();
    ZipEntry copyZipEntry = new ZipEntry(zipEntry.getName());
    zos.putNextEntry(copyZipEntry);
    if(!zipEntry.isDirectory()) {
      InputStream in = src.getInputStream(zipEntry);
      int len;
      byte[] buffer = new byte[8 * 1024];
      while ((len = in.read(buffer)) != -1) {
        zos.write(buffer, 0, len);
      }
    }
    zos.closeEntry();
  }
}
```

就这么简单几十行的代码就能释放我们双手，瞬间自动地打出多个甚至几十个渠道包了！但似乎读取渠道文件时稍稍有点耗时，因为要遍历整个`Apk`文件，如果文件一大，性能可能就不太理想了，有没更好的方法？答案肯定是有的，我们接下来看看第三种方案。

### 2.3 `APK`文件末尾追加渠道注释

在探索这个方案前，你需要了解zip文件的格式，大家可以参考下文章 [ZIP文件格式分析](https://link.jianshu.com?t=https%3A%2F%2Fblog.csdn.net%2Fa200710716%2Farticle%2Fdetails%2F51644421)。内容很多，记不住？没关系，该方案你只需关注`zip`文件的末尾的格式` End of central directory record (EOCD）`：

| Offset | Bytes | Description                                                  |
| ------ | ----- | ------------------------------------------------------------ |
| 0      | 4     | `End of central directory signature = 0x06054b50`            |
| 4      | 2     | `Number of this disk`                                        |
| 6      | 2     | `Number of the disk with the start of the central directory` |
| 8      | 2     | `Total number of entries in the central directory on this disk` |
| 10     | 2     | `Total number of entries in the central directory`           |
| 12     | 4     | `Size of central directory(bytes)`                           |
| 16     | 2     | `Offset of start of central directory with respect to the starting disk number` |
| 20     | 2     | `Comment legnth(n)`                                          |
| 22     | n     | `Comment`                                                    |

zip文件末尾的字节 **`Comment`** 就是其注释。我们知道，代码的注释是不会影响程序的，它只是为代码添加说明。zip的注释同样如此，它并不会影响zip的结构，在注释了写入字节，对Apk文件不会有任何影响，也即能正常安装。

基于此特性，我们就可以在zip的注释块里动手了，可以在注释里写入我们的渠道信息，来区分每个渠道包。但需要注意的是：**`Comment Length`** 所记录的注释长度必须跟实际所写入的注释字节数相等，否则`Apk`文件安装会失败。

#### 2.3.1 追加渠道注释

追加注释就是在文件末写入数据而已。但我们要有一定的格式，来标识是我们自己写的注释，并且能保证我们能正确地读取渠道号。为了简单起见，我们里使用的格式也很简单：

| Offset | Bytes | Description          |
| ------ | ----- | -------------------- |
| 0      | n     | `Json`格式的渠道信息 |
| n      | 2     | 渠道信息的字节数     |
| n+2    | 3     | 魔数“CHA”，标记作用  |

写入注释时只需要注意更新**`Comment Length`**的字节数就可以了：

```java
public static void writeFileComment(File apkFile, String data) {
  if(apkFile == null) {
    throw new NullPointerException("Apk file can not be null");
  }
  if(!apkFile.exists()) {
    throw new IllegalArgumentException("Apk file is not found");
  }
  int length = data.length();
  if(length > Short.MAX_VALUE) {
    throw new IllegalArgumentException("Size out of range: " + length);
  }

  RandomAccessFile accessFile = null;
  try {
    accessFile = new RandomAccessFile(apkFile, "rw");
    long index = accessFile.length();
    index -= 2; // 2 = FCL
    accessFile.seek(index);

    short dataLen = (short) length;
    int tempLength = dataLen + BYTE_DATA_LEN + COMMENT_MAGIC.length();
    if(tempLength > Short.MAX_VALUE) {
      throw new IllegalArgumentException("Size out of range: " + tempLength);
    }

    short fcl = (short) tempLength;
    // Write FCL
    ByteBuffer byteBuffer = ByteBuffer.allocate(Short.BYTES);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.putShort(fcl);
    byteBuffer.flip();
    accessFile.write(byteBuffer.array());

    // Write data
    accessFile.write(data.getBytes(CHARSET));

    // Write data len
    byteBuffer = ByteBuffer.allocate(Short.BYTES);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.putShort(dataLen);
    byteBuffer.flip();
    accessFile.write(byteBuffer.array());

    //Write flag
    accessFile.write(COMMENT_MAGIC.getBytes(CHARSET));
  }catch (Exception e) {
    e.printStackTrace();
  }finally {
    IOUtils.closeQuietly(accessFile);
  }
}
```

#### 2.3.2 读取渠道注释

因为不用遍历文件，读取渠道注释就比方式一的渠道方式快多了，只要根据我们自己写入文件的注释格式，从文件末逆着读就可以了（嘻嘻，这你知道我们为何在写入注释时需要写入我们渠道信息的长度了吧～）。好，看代码：

```java
public static String readFileComment(File apkFile) {
  if(apkFile == null) {
    throw new NullPointerException("Apk file can not be null");
  }
  if(!apkFile.exists()) {
    throw new IllegalArgumentException("Apk file is not found");
  }

  RandomAccessFile accessFile = null;
  try {
    accessFile = new RandomAccessFile(apkFile, "r");
    FileChannel fileChannel = accessFile.getChannel();
    long index = accessFile.length();

    // Read Flag
    index -= COMMENT_MAGIC.length();
    fileChannel.position(index);
    ByteBuffer byteBuffer = ByteBuffer.allocate(COMMENT_MAGIC.length());
    fileChannel.read(byteBuffer);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    if(!new String(byteBuffer.array(), CHARSET).equals(COMMENT_MAGIC)) {
      return "";
    }

    // Read dataLen
    index -= BYTE_DATA_LEN;
    fileChannel.position(index);
    byteBuffer = ByteBuffer.allocate(Short.BYTES);
    fileChannel.read(byteBuffer);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    short dataLen = byteBuffer.getShort(0);

    // Read data
    index -= dataLen;
    fileChannel.position(index);
    byteBuffer = ByteBuffer.allocate(dataLen);
    fileChannel.read(byteBuffer);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return new String(byteBuffer.array(), CHARSET);
  }catch (Exception e) {
    e.printStackTrace();
  }finally {
    IOUtils.closeQuietly(accessFile);
  }
  return "";
}
```

#### 2.3.3 生成多个渠道包

这部分就跟方式一的差不多了，只是处理的方式不同而已，就不多说了：

```java
public static void addChannelToApk(File apkFile) {
  if(apkFile == null) {
    throw new NullPointerException("Apk file can not be null");
  }

  Map<String, String> channels = getAllChannels();
  Set<String> channelSet = channels.keySet();
  String srcApkName = apkFile.getName().replace(".apk", "");

  InputStream in = null;
  OutputStream out = null;
  for (String channel : channelSet) {
    String channelId = channels.get(channel);
    String jsonStr = "{" +
      "\"channel\":" + "\"" + channel + "\"," +
      "\"channel_id\":" + "\"" + channelId + "\"" +
      "}";
    try {
      File channelFile = new File(BUILD_DIR,
                                  srcApkName + "_" + channel + "_" + channelId + ".apk");
      if(channelFile.exists()) {
        channelFile.delete();
      }
      FileUtils.createNewFile(channelFile);
      in = new FileInputStream(apkFile);
      out = new FileOutputStream(channelFile);
      copyApkFile(in, out);

      FileCommentProcessor.writeFileComment(channelFile, jsonStr);
    }catch (Exception e) {
      e.printStackTrace();
    }finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }
}

/**
 * 把输入流中的数据写入输出流
 * @param in
 * @param out
 * @throws IOException
 */
private static void copyApkFile(InputStream in, OutputStream out) throws IOException {
  byte[] buffer = new byte[4 * 1024];
  int len;
  while ((len = in.read(buffer)) != -1) {
    out.write(buffer, 0, len);
  }
}
```

注意，上面的实例没有考虑Apk原本存在注释的情况，如果要考虑的话，可以根据`EOCD的`开始标记，值是固定为 `0x06054b50`，找到这个标记，再相对偏移20的字节就是 `Comment Length`，这样就能知道原有注释的长度了。

### 2.4 针对`Android7.0`新增的`V2`签名方案的`APK`添加渠道`ID-value`

该方案的思路主要分为两步：首先需要找到`APK Signing Block`数据块，然后对`ID-value`进行扩展，写入包含渠道信息的`ID-value`。详情可参考`1.5.2 方案落地`。

#### 2.4.1 寻找`APK Signing Block`数据块

由`1.5.2`可知，`APK Signing Block`是在紧接着`Contents of ZIP entries`之后，在`Central Directory`之前，通过`Zip`的 `End of central directory record(EOCD)`可以知道`Central Directory`的具体位置，即`Offset of start of central directory relative to start of archive`存储的4个字节，而`Central Directory`是紧跟着`APK signing Block`的，所以可以通过`Central Directory`找到签名块的具体位置：

```java
public static long findCentralDirStartOffset(final FileChannel fileChannel, final long commentLength) throws IOException {
  // End of central directory record (EOCD)
  // Offset    Bytes     Description[23]
  // 0           4       End of central directory signature = 0x06054b50
  // 4           2       Number of this disk
  // 6           2       Disk where central directory starts
  // 8           2       Number of central directory records on this disk
  // 10          2       Total number of central directory records
  // 12          4       Size of central directory (bytes)
  // 16          4       Offset of start of central directory, relative to start of archive
  // 20          2       Comment length (n)
  // 22          n       Comment
  // For a zip with no archive comment, the
  // end-of-central-directory record will be 22 bytes long, so
  // we expect to find the EOCD marker 22 bytes from the end.

  final ByteBuffer zipCentralDirectoryStart = ByteBuffer.allocate(4);
  zipCentralDirectoryStart.order(ByteOrder.LITTLE_ENDIAN);
  fileChannel.position(fileChannel.size() - commentLength - 6); // 6 = 2 (Comment length) + 4 (Offset of start of central directory, relative to start of archive)
  fileChannel.read(zipCentralDirectoryStart);
  final long centralDirStartOffset = zipCentralDirectoryStart.getInt(0);
  return centralDirStartOffset;
}
```

再根据`Central Directory`的位置，向上读`APK Signing Block`:

```java
public static Pair<ByteBuffer, Long> findApkSigningBlock(
  final FileChannel fileChannel, final long centralDirOffset) throws IOException, SignatureNotFoundException {

  // Find the APK Signing Block. The block immediately precedes the Central Directory.

  // FORMAT:
  // OFFSET       DATA TYPE  DESCRIPTION
  // * @+0  bytes uint64:    size in bytes (excluding this field)
  // * @+8  bytes payload
  // * @-24 bytes uint64:    size in bytes (same as the one above)
  // * @-16 bytes uint128:   magic

  if (centralDirOffset < APK_SIG_BLOCK_MIN_SIZE) {
    throw new SignatureNotFoundException(
      "APK too small for APK Signing Block. ZIP Central Directory offset: "
      + centralDirOffset);
  }
  // Read the magic and offset in file from the footer section of the block:
  // * uint64:   size of block
  // * 16 bytes: magic
  fileChannel.position(centralDirOffset - 24);
  final ByteBuffer footer = ByteBuffer.allocate(24);
  fileChannel.read(footer);
  footer.order(ByteOrder.LITTLE_ENDIAN); // 小端模式，高字节保存在高地址
  // 是否存在V2签名魔数：APK Sig Block 42
  if ((footer.getLong(8) != APK_SIG_BLOCK_MAGIC_LO)
      || (footer.getLong(16) != APK_SIG_BLOCK_MAGIC_HI)) {
    throw new SignatureNotFoundException(
      "No APK Signing Block before ZIP Central Directory");
  }
  // Read and compare size fields
  final long apkSigBlockSizeInFooter = footer.getLong(0); // 签名块的总长度
  if ((apkSigBlockSizeInFooter < footer.capacity())
      || (apkSigBlockSizeInFooter > Integer.MAX_VALUE - 8)) {
    throw new SignatureNotFoundException(
      "APK Signing Block size out of range: " + apkSigBlockSizeInFooter);
  }
  final int totalSize = (int) (apkSigBlockSizeInFooter + 8); // + 8 （签名块第一个Block长度字节数）
  final long apkSigBlockOffset = centralDirOffset - totalSize;
  if (apkSigBlockOffset < 0) {
    throw new SignatureNotFoundException(
      "APK Signing Block offset out of range: " + apkSigBlockOffset);
  }
  fileChannel.position(apkSigBlockOffset);
  final ByteBuffer apkSigBlock = ByteBuffer.allocate(totalSize);
  fileChannel.read(apkSigBlock);
  apkSigBlock.order(ByteOrder.LITTLE_ENDIAN);
  final long apkSigBlockSizeInHeader = apkSigBlock.getLong(0);
  if (apkSigBlockSizeInHeader != apkSigBlockSizeInFooter) { // 再检验一次，真严格！
    throw new SignatureNotFoundException(
      "APK Signing Block sizes in header and footer do not match: "
      + apkSigBlockSizeInHeader + " vs " + apkSigBlockSizeInFooter);
  }
  return Pair.of(apkSigBlock, apkSigBlockOffset);
}
```

#### 2.4.2 对`ID-value`进行扩展，写入包含渠道信息的`ID-value`

先拿出原来`APK`已存在的`ID-value`，然后把我们自己的渠道信息保存在新的`ID-vlaue`中，再把新的旧的`ID-value`一起写入`APK`：

```java
public static void writeApkSigningBlock(final File apkFile, final Map<Integer, ByteBuffer> idValues) throws IOException, SignatureNotFoundException {
  RandomAccessFile fIn = null;
  FileChannel fileChannel = null;

  try {
    fIn = new RandomAccessFile(apkFile, "rw");
    fileChannel = fIn.getChannel();
    //获取注释长度
    final  long commentLength = ApkUtil.getCommentLength(fileChannel);
    //获取核心目录偏移
    final long centralDirStartOffset = ApkUtil.findCentralDirStartOffset(fileChannel, commentLength);
    final Pair<ByteBuffer, Long> apkSigningBlockAndOffset
      = ApkUtil.findApkSigningBlock(fileChannel, centralDirStartOffset); //获取签名块
    final ByteBuffer oldApkSigningBlock = apkSigningBlockAndOffset.getmFirst();
    final long apkSigningBlockOffset = apkSigningBlockAndOffset.getmSecond();

    //获取apk已有的ID-value
    final Map<Integer, ByteBuffer> originIdValues = ApkUtil.findIdValues(oldApkSigningBlock);
    //查找apk的签名信息，ID值固定为：0x7109871a
    final ByteBuffer apkSignatureSchemeV2Block = originIdValues.get(ApkUtil.APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
    if(apkSignatureSchemeV2Block == null) {
      throw new IOException("No APK Signature Scheme v2 block in APK Signing Block");
    }

    //获取所有 ID-value
    final ApkSigningBlock apkSigningBlock = genApkSigningBlock(idValues, originIdValues);

    if(apkSigningBlockOffset != 0 && centralDirStartOffset != 0) {
      //读取核心目录的内容
      fIn.seek(centralDirStartOffset);
      byte[] centralDirBytes;
      centralDirBytes = new byte[(int) (fileChannel.size() - centralDirStartOffset)];
      fIn.read(centralDirBytes);

      //更新签名块
      fileChannel.position(apkSigningBlockOffset);
      //写入新的签名块，返回的长度是不包含签名块头部的 Size of block (8字节)
      final long lengthExcludeHSOB = apkSigningBlock.writeApkSigningBlock(fIn);

      //更新核心目录
      fIn.write(centralDirBytes);

      //更新文件的总长度
      fIn.setLength(fIn.getFilePointer());

      // 更新 EOCD 所记录的核心目录的偏移
      // End of central directory record (EOCD)
      // Offset     Bytes     Description[23]
      // 0            4       End of central directory signature = 0x06054b50
      // 4            2       Number of this disk
      // 6            2       Disk where central directory starts
      // 8            2       Number of central directory records on this disk
      // 10           2       Total number of central directory records
      // 12           4       Size of central directory (bytes)
      // 16           4       Offset of start of central directory, relative to start of archive
      // 20           2       Comment length (n)
      // 22           n       Comment

      fIn.seek(fileChannel.size() - commentLength - 6);
      // 6 = 2(Comment length) + 4(Offset of start of central directory, relative to start of archive)
      final ByteBuffer temp = ByteBuffer.allocate(4);
      temp.order(ByteOrder.LITTLE_ENDIAN);
      long oldSignBlockLength = centralDirStartOffset - apkSigningBlockOffset; //旧签名块字节数
      long newSignBlockLength = lengthExcludeHSOB + 8; //新签名块字节数，8 = size of block in bytes (excluding this field) (uint64)
      long extraLength = newSignBlockLength - oldSignBlockLength;
      temp.putInt((int) (centralDirStartOffset + extraLength));
      temp.flip();
      fIn.write(temp.array());

    }
  } finally {
    IOUtils.closeQuietly(fileChannel);
    IOUtils.closeQuietly(fIn);
  }
}

private static ApkSigningBlock genApkSigningBlock(Map<Integer, ByteBuffer> idValues,
                                                  Map<Integer, ByteBuffer> originIdValues) {
  //把已有的和新增的 ID-value 添加到 payload 列表
  if(idValues != null && !idValues.isEmpty()) {
    originIdValues.putAll(idValues);
  }
  final ApkSigningBlock apkSigningBlock = new ApkSigningBlock();
  final Set<Map.Entry<Integer, ByteBuffer>> entrySet = originIdValues.entrySet();
  for (Map.Entry<Integer, ByteBuffer> entry : entrySet) {
    final ApkSigningPayload payload = new ApkSigningPayload(entry.getKey(), entry.getValue());
    apkSigningBlock.addPayload(payload);
  }
  return apkSigningBlock;
}
```

注意上面在写完`ID-value`后，因为`APK Signing Block`的长度变化了，相应的`APK`文件大小和`Central Directory`的偏移也会变化，需要同步更新。

## 三、参考资料

本文参考：

[Android多渠道包生成最佳实践（一）](https://www.jianshu.com/p/1b45fe3db67e)

[Android多渠道包生成最佳实践（二）](https://www.jianshu.com/p/3ba40dd91118)


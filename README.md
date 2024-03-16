## 一、前言

在介绍Togr之前先了解一下内网穿透

### 内网穿透

根据网络的公共访问性可分简单的分为“内网”、“公网”两种类型的网络。

内网指的是只有在局域网中且在同一个网段的主机才能相互访问，这就导致了一个局限性，如果两台主机不在同一个网段那么双方就很难进行直接的网络通信。

由于IPv4只有4,294,967,296个地址，且因为某些用于特殊用途保留的和其它所导致的原因，实际上能给用户使用的IPv4的地址达不到42亿个，所以就会面临IP枯竭的问题，所以在办理宽带时运营商为了稍微缓解IP数量不足的问题，大部分的用户所办理的宽带账号所获得的IP都是动态IP，也就是每次连上互联网后自己的IP不一定是上一次所使用的，这种IP一般不具有别人能跨网段访问的能力，也就不是“公网”，只有通过特殊手段或者企业办理的宽带才是固定的IP且是公网。

内网穿透简单来说就是指让内网也能具备公网一样的能力，使得别的主机也可以直接访问内网的主句，最常见的就是服务器中转数据穿透，就是利用有公网IP的服务器作为数据中转。

## 二、开发背景

一般人很难直接申请到公网IP，但是现在有种产品已经特别常见并且自带固定的公网IP，也就是云服务器，现在的云服务器价格可以说是十分的亲民，动不动就是99一年。

但是一般这种比较便宜的云服务器有个比较大的问题就是一般内存都比较小

![image.png](https://b3logfile.com/file/2024/03/image-CLSTGTb.png)

这是一个2G内存的服务器，在去除centos系统本身所占用的内存，剩下的可用的内存就几乎所剩无几了，很难再运行什么别的，比如Minecraft服务器或者幻兽帕鲁服务器动辄就是4G8G甚至16G的内存占用。

但是又考虑到有些人手上有闲置的电脑主机等，且一般配置都会比云服务器好很多，所以完全可以将云服务器作为内网穿透的一个媒介。

## 三、使用介绍

首先先从Togr仓库Clone出来（可以的话麻烦点个小小的star）

[FT-Fetters/Togr: Intranet penetration (github.com)](https://github.com/FT-Fetters/Togr)

由于本项目的网络通信框架基于我的另一个项目，所以再打包这个项目之前需要Clone另一个项目也进行本地打包后生成相关依赖，当然为了方便我也会直接提供打包好的jar包（[Togr-jar.zip](http://ldqc.xyz/#s/-MswY1dw)）

### 手动打包

```shell
# clone tightly call 到本地
git clone https://github.com/FT-Fetters/TightlyCall.git
# 切换到最新分支
git checkout dev-1.0.1
# 将tightly call 打包至本地maven仓库
mvn clean install
```

```shell
# clone togr 到本地
git clone https://github.com/FT-Fetters/Togr.git
# 打包Togr
mvn clean install
```

将上面的命令执行完后就能得到一个TogrClient-1.0-SNAPSHOT-jar-with-dependencies.jar和TogrServer-1.0-SNAPSHOT-jar-with-dependencies.jar

下面统称为client和server

### 搭建

首先我们需要将server.jar上传至我们的云服务器（或者具有公网IP的主机）

确保云服务器已经安装和配置了Java运行环境，可以通过java -version来确定是否安装了

```shell
# 查看已开放的端口
firewall-cmd --zone=public --list-ports
# 开放8082和7086端口
firewall-cmd --zone=public --add-port=8082/tcp --permanent
firewall-cmd --zone=public --add-port=7086/tcp --permanent
```

通过以上两条命令我们需要开放一个用来连接的端口（系统是centos，如果是其它系统可能不一样），这里我选用了8082，当然其它可用端口也可以，同时我们需要保证我们服务器上的7086端口没有被其它应用所占用，同时如果是买的云服务器也需要去官网上的控制台防火墙打开相应的端口

![image.png](https://b3logfile.com/file/2024/03/image-qxi0CO6.png)

然后我们需要切换到上传server.jar的目录然后运行以下命令

```shell
# Togrserver.jar 为你上传云服务器server.jar的文件名	8082为你开放的端口
java -jar TogrServer.jar 8082
```

当看到server running on port xxxx时就代表已经启动成功了

接下来切换到我们的内网主机

我们需要先启动我们需要进行内网穿透端口映射的应用，这里我拿Minecraft服务器作为例子

![image.png](https://b3logfile.com/file/2024/03/image-zY28dAL.png)

可以看到启动的端口是25565

然后切换到我们client.jar所在的目录

并且执行以下命令

```shell
# xxx.xxx.157.94是服务器的ip地址或域名 25565是我们内网应用的端口
java -jar .\TogrClient.jar xxx.xxx.157.94 7086 127.0.0.1 25565
```

![image.png](https://b3logfile.com/file/2024/03/image-kRbqSyi.png)

当我们看到服务器上出现Taget online，这个时候我们的内网穿透基本就搭建完毕了

### 测试

打开我们的Minecraft，并输入xxx.xxx.157.94:8082

![image.png](https://b3logfile.com/file/2024/03/image-Vi7wfCi.png)

发现已经可以连接到了，延迟取决于服务器的连接延迟

## 四、结语

目前只实现的TCP的连接，测试了包括像Minecraft以及http等也是没有问题的，所以像palworld应该也是没有问题的，由于实现的技术手段，数据转发过程必然会有性能损失，且还要进行多次的互联网数据中转，也会导致一定的延迟，所以主要就是看中转服务器到内网主机以及客户端到服务器的网络延迟。

后续文章会对Togr的技术细节进行说明，希望能点个小小的star

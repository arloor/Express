# Express
简单的http代理——使用socket编程实现。


## HttpProxy   
使用socket实现http代理：     
请求：本地浏览器-->代理（本程序）-->远程服务器     
响应：本地浏览器<--代理（本程序）<--远程服务器    


已知问题：    
1.因为对每一个请求的处理是调用线程池的一个线程，创建代理远程的socket，也就是每一次请求（一个页面、一张图片、一个css）都要去建立tcp连接，进行三次握手，这消耗了大量的时间，因此特别的慢。这个问题大致知道怎么解决（维护一个socket列表）     
2.对https的支持问题。尽管使用了SSLSocket但是因为证书的问题，导致ssl认证失败，这方面还有待研究。


解决问题一的思路：  
在accept本地浏览器请求时，把获得的本地socket保存到map中（key是请求的host域名）  
同时在建立与远程服务器的连接时同样在另一个map中，key也是host域名   
然后创建一个线程，使用特定的方法（需要学习）循环检测这些socket的远程端是否关闭。关闭则从map中删除本地和remote socket。  
这样需要修改main的实现为使用select+多线程。   
就是：将map中的所有本地浏览器socket和代理的serverSocket注册到select中。serversocket就绪就accept。其他socket就绪就执行Thread来读取请求并发送请求。
另外select所有的remote socket来传递远程的响应。

# Express
简单的http代理——使用socket编程实现。


## HttpProxy   
使用socket实现http代理：     
请求：本地浏览器-->代理（本程序）-->远程服务器     
响应：本地浏览器<--代理（本程序）<--远程服务器    
    
已知问题：    
1.因为对每一个请求的处理是调用线程池的一个线程，创建代理远程的socket，也就是每一次请求（一个页面、一张图片、一个css）都要去建立tcp连接，进行三次握手，这消耗了大量的时间，因此特别的慢。这个问题大致知道怎么解决（维护一个socket列表）     
2.对https的支持问题。尽管使用了SSLSocket但是因为证书的问题，导致ssl认证失败，这方面还有待研究。     

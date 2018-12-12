# Davinci 一个更轻量的图片加载框架

## v0.1  
- [x] 给定Url加载出图片
- [x] 支持内存缓存和磁盘缓存
- [x] 支持并发操作（采用Kotlin协程，资源利用更轻量、代码可读性更友好）
- [x] 支持android平台的生命周期事件、页面销毁后相关请求会自动取消

## v0.2
- [x] 支持目标view大小的图片缓存
- [ ] 支持不同版本的bitmap解码策略、内存更友好
- [ ] 支持图片转换操作（变化和裁剪）
- [ ] 支持网络库切换
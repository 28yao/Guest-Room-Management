技术栈要求：

前端：Vue3 + Element Plus
后端：SpringBoot + MyBatis-Plus
数据库：MySQL


Java 所有类必须包含标准JavaDoc注释

```java
/**
* 类功能描述(说明该类核心职责)
*
* @author liuxinsi
* @date生成注释的时间规则:
*/
```


规则：

1.必须使用 JavaDoc 风格(/***/)
2.必须描述类的主要职责
3.禁止省略类注释
4.禁止使用嵌套循环
5.使用java8环境

Java 所有接口必须包含标准JavaDoc注释


需求database: 

 - 禁止生成DROP TABLE/DROP DATABASE语句 
 - 所有SQL必须使用参数化查询 
 - 操作前必须验证环境（生产/测试/开发） 
 - 危险操作必须添加二次确认机制

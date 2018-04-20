# xmysql-connector-j
	mysql6.0.6驱动，启用useServerPrepStmts，让preparedStatement在服务端缓存，减少sql解析开销，提高性能。
	在某些情况下查询出错，本工程修复该问题。
	
下载已修复的驱动：[dist/fixed/mysql-connector-java-6.0.6.jar](dist/fixed/mysql-connector-java-6.0.6.jar)

[https://forums.mysql.com/read.php?39,665663](https://forums.mysql.com/read.php?39,665663)

该问题在官方刚出的8.0.11版仍存在。

## 出错情况

select * ...语句被缓存后，如果表结构发生变化，则再次查询驱动会报异常(ArrayIndexOutOfBoundsException)。



## 原因分析

mysql6.0.6的驱动，不把查询出的Fields与缓存的preparedStatement中的Fields进行merge(驱动为解决某些field特殊情况下preparedStatement无flags，而查询返回的结果有flags做出的merge)。


## 解决办法

直接用查询结果的fields，修改类MergingColumnDefinitionFactory。

## myql url优化参数说明

```java
// 通用
final String common = "&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8";

// 1.优化为fetch分批查询
final String fetch = "&useCursorFetch=true&useServerPrepStmts=true&rewriteBatchedStatements=false&cachePrepStmts=true&prepStmtCacheSize=1024&prepStmtCacheSqlLimit=4096";

// 2.优化批量插入
final String batch = "&useServerPrepStmts=false&rewriteBatchedStatements=true&useCompression=true";


// 两者不可同时优化: useCursorFetch=true需要useServerPrepStmts=true，而useServerPrepStmts与rewriteBatchedStatements不能同时为true。

```

### 参数生效的依赖

useCursorFetch=true依赖useServerPrepStmts=true，实际上只要设置了useCursorFetch=true，useServerPrepStmts不管设置true/false，都会被默认为是true。

useServerPrepStmts=true依赖cachePrepStmts=true。


若关闭cachePrepStmts，会导致useCursorFetch不生效，查询大数据是可能oom。
启用cachePrepStmts后，preparedSatement只有在其connection真正关闭才会释放，若用连接池则在连接池close之前一直存在。


### 修复另一个bug
useServerPrepStmts启用的情况下：

getString读取null值的text、longtext字段，getTimestamp读取null值字段(可以不是timestamp类型)时，则抛ArrayIndexOutOfBoundsException，
使用getObject则可避免，因为getObject有getNull检测。

BLOB、JSON类型也应有类似问题，@see MysqlaUtils.getBinaryEncodedLength返回0的case。

在此已修复避免抛异常，修改：com.mysql.cj.mysqla.result.BinaryBufferRow#getValue。

[https://forums.mysql.com/read.php?39,665662](https://forums.mysql.com/read.php?39,665662)

该bug在官方刚出的8.0.11版本中已修复。
# <font color=#FF0000 face="微软雅黑">msio</font>
### 服务管理，控制ms系列数据导入导出便捷式框架
通过实现AbstractMsConfigure进行配置，现有添加拦截器以及添加映射的方法供重写修改。
#### 注解
1. @MsAutomatic:自增项，在解析中无需在Excel中指定，会自动生成项，适用于自动生成ID和当前时间，value指定当前解析项，isUseNowDate是使用当前时间，isUseUUID是UUID自动生成
2. @MsIgnore:如其名，不进行导出导入映射操作
3. @MsItem:对映射字段进行详细解释的注解，value代表其中文映射，methodName会调用导出类型转换容器中的同名方法，方法的参数均为java.lang.String，返回参数均为java.lang.Object，为了更好的解释型操作，额外添加一个Operator接口，Operator有内置方法，如果有内置方法的话会优先使用内置方法进行转换。
4. @MsOperator:操作项，声明此Pojo为一个映射的注解，tableName指定当前Pojo对应的数据库相应字段，暂时未实现，可暂时不使用，value为当前映射集中唯一的编码，可通过这个编码指定映射集，subClazz用于复杂导出的时候解释该Pojo内含有的其他Pojo属性，在填入时去重。
5. @MsPackageScan:Package扫描指定路径，相当于ComponentScan注解，不过这个仅仅应用于映射获取。
6. @MsReturnTranslator:在需要导出的controller的方法上进行注解，非必要注解（不注解也可以导出），当然，这并不是无效的，其中的value属性时一个简单实现的EL表达式，用于提取返回的如果不是List的情况下进行格式调整，如被封装在一个Page对象中的List，就可以通过这个value进行提取，如下例：**@MsReturnTranslator("getPage()#getList($int$5,$int$150,wowowo,$double$1.41)")**
_例子中#表示方法调用序列，相当于源码中的.号进行方法调用，第一个可省略，参数仅支持int，double，String三种，需要在参数前加上$int$（转化为int类型），$double$（转换为double类型），不加是默认的String类型，方法一直调用直至返回为需要导出的List数据即可，在导出时仅仅支持List转换，因此如果返回类型不是List的话，不会触发导出操作，依旧进行正常的接口返回操作_  
id是指定的导出id集，复杂导出时必须指定，设计为String[]类型为导出多种类型的组合单次导出（多页非相同的映射）
isComplex是否为复杂项导出，在需要复杂导出的时候必须要指定该属性为true。
#### 基本转换方法
1. IFormatConversion:导入时自动转换类型的方法，java.lang.Object fromStringtoXXX(java.lang.String)是固定的方法名称，XXX为类型简称，例如java.lang.Integer就是fromStringtoInteger方法。自动调用，如果出现两个类简称相同的情况下，可以写入类全名去除所有的‘.’,例如刚刚的例子全称就是fromStringtojavalangInteger，通过
这样的方法来保证其唯一性（推荐在接收值的时候进行判空）。
2. ITransFunctionContainer:导出时指定使用的方法集，通过指定的方式调用，在


# <font face="微软雅黑">msio</font>
**_服务管理，控制ms系列数据导入导出便捷式框架_**


## Version 1.0.0
通过实现AbstractMsConfigure进行配置，现有添加拦截器以及添加映射的方法供重写修改。

application.yml/application.properties中的配置项

```properties
#是否热部署，每次配置是否重新更新（注意：选择为热部署的情况，也仅会更新配置文件部分数据）,true为每次更新
spring.msIo.isHotCache=true
#是否开启servlet自动解析，如果不开启，需要手动调用解析方法解析(默认开启)
spring.msIo.autoServlet=true
#是否开启上传下载记录储存(暂时未实现)
spring.msIo.dbLog=true
#下载监听路径抬头
spring.micro.listen.url=/upload/*
```

### 注解
1. @MsAutomatic:自增项，在解析中无需在Excel中指定，会自动生成项，适用于自动生成ID和当前时间，value指定当前解析项，isUseNowDate是使用当前时间，isUseUUID是UUID自动生成
2. @MsIgnore:如其名，不进行导出导入映射操作
3. @MsItem:对映射字段进行详细解释的注解，value代表其中文映射，methodName会调用导出类型转换容器中的同名方法，方法的参数均为java.lang.String，返回参数均为java.lang.Object，为了更好的解释型操作，额外添加一个Operator接口，Operator有内置方法，如果有内置方法的话会优先使用内置方法进行转换。
4. @MsOperator:操作项，声明此Pojo为一个映射的注解，tableName指定当前Pojo对应的数据库相应字段，暂时未实现，可暂时不使用，value为当前映射集中唯一的编码，可通过这个编码指定映射集，subClazz用于复杂导出的时候解释该Pojo内含有的其他Pojo属性，在填入时去重。
5. @MsPackageScan:Package扫描指定路径，相当于ComponentScan注解，不过这个仅仅应用于映射获取。
6. @MsReturnTranslator:在需要导出的controller的方法上进行注解，非必要注解（不注解也可以导出），当然，这并不是无效的，其中的value属性时一个简单实现的EL表达式，用于提取返回的如果不是List的情况下进行格式调整，如被封装在一个Page对象中的List，就可以通过这个value进行提取，如下例：**@MsReturnTranslator("getPage()#getList($int$5,$int$150,wowowo,$double$1.41)")**
_例子中#表示方法调用序列，相当于源码中的.号进行方法调用，第一个可省略，参数仅支持int，double，String三种，需要在参数前加上$int$（转化为int类型），$double$（转换为double类型），不加是默认的String类型，方法一直调用直至返回为需要导出的List数据即可，在导出时仅仅支持List转换，因此如果返回类型不是List的话，不会触发导出操作，依旧进行正常的接口返回操作_  
id是指定的导出id集，复杂导出时必须指定，设计为String[]类型为导出多种类型的组合单次导出（多页非相同的映射）
isComplex是否为复杂项导出，在需要复杂导出的时候必须要指定该属性为true。
### 基本转换方法
1. IFormatConversion:导入时自动转换类型的方法，java.lang.Object fromStringtoXXX(java.lang.String)是固定的方法名称，XXX为类型简称，例如java.lang.Integer就是fromStringtoInteger方法。自动调用，如果出现两个类简称相同的情况下，可以写入类全名去除所有的‘.’,例如刚刚的例子全称就是fromStringtojavalangInteger，通过
    这样的方法来保证其唯一性（推荐在接收值的时候进行判空），重写的话需要注册为组件。

2. ITransFunctionContainer:导出时指定使用的方法集，通过指定的方式调用，在MsItem指定的method就是方法调用就是基于该类的实现类，需要注册为组件。

### 文件配置

在项目的resource的文件夹下添加msio.json文件。

简单Excel映射格式如下

```json
{
  "1":{
    "xhz":"小\\$$t1",
    "dhz":"大",
    "className":"com.github.test.Test"  },
  "3":{
    "//className":"github.test.Test" }
}
```

配置是通过json实现的，第一层的json的key：value集就是每一个映射实体，key为映射环境下的唯一id，如同pojo映射中的MsOperator的value值，内部为英文字段和中文字段的映射关系。  
<font color=#FF2000 size=3>双$标识此字段导出时需要调用ITransFunctionContainer中的指定双$后面的方法，注意标注在中文字段的最后，不然导致方法无法找到并且双$后面的字段无法展示，如果实在需要用到双$,可通过\\\\进行强制转换。  
className为指定该映射为某一个Pojo类的映射，如果不需要不要填写，如果填写后此映射就如同在pojo类中注解一样，返回的List中元素的类型会是Pojo对象而不是Map，和双$相同，可以被转义</font>

复杂的Excel映射的例子

```json
{
  "2":{
    "min":{
      "name":"基本信息",
      "\\name":"名称",
      "age":"年龄",
      "sex":"性别",
      "hobbies":{
        "name":"爱好",
        "hobby1":"爱好1",
        "hobby2":"爱好2",
        "hobby3":"爱好3"
      },
      "temp":{
        "name":"你猜",
        "id":"1"
      }
    },
    "id":"标识信息",
    "level":"评级"
  }
}
```

复杂Excel因为并非一对一关系，一个集可能内部包含多个集，而不是简单Excel中，一个集中存在的都是一对一的简单元素。因此对单映射中的子集做下规范，英文标识后如果跟随的是一个子集，取子集中的name属性作为该字段的中文对应名称，解析中会将子集作为一个映射集存储映射池，所以子集的key理应保证其唯一性，**子集**中的id有特殊定义，id指向json中该集上方的某一个映射id等于这个id的值，如果没有找到，会报错，找到后会将该集直接覆盖这个集的映射，当然name还是需要作为中文对应而存在的，依旧可以通过\\\进行转义。

### 正式使用

1. 下载excel：在访问接口时在前面加上监听路径抬头设置的例如/download即可进行下载

2. 上传excel：上传接口由于涉及数据更改，因此不提供自动上传解析等操作，需要手动实现代码。具体例子如下：

   ```java
   public void test2(MultipartFile file){
       List data = ExcelFactory.getSingleSimpleInstance(file).getData(0);
       System.out.println(data);
   }
   ```

   可以通过ExcelFactory方法构建导出单元然后直接通过getData（java.lang.Integer）方法进行获取，参数的Integer表示页数，即如果Excel有多个工作簿，可以根据这个方法进行多页数据的获取，从0开始。

   <font color=#408F00>ExcelFactory提供了大量的生成处理单元的方法，根据不同的参数进行生成单元处理，如果不需要自动Servlet进行导出的话，可以关闭功能，通过ExcelFactory进行手动创建接口导出，会返回一个workbook对象，然后通过对response的回写可以实现自助的导出功能</font>

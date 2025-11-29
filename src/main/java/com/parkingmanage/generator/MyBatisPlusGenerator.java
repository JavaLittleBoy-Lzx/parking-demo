package com.parkingmanage.generator;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.Scanner;


public class MyBatisPlusGenerator {
    public static String scanner(String tip) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder help = new StringBuilder();
        help.append("请输入" + tip + "：");
        System.out.println(help.toString());
        if (scanner.hasNext()) {
            String ipt = scanner.next();
            if (StringUtils.isNotBlank(ipt)) {
                return ipt;
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();
        //table 表1
        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath + "/src/main/java");
        gc.setAuthor("lzx");//作者
        gc.setFileOverride(true); // 文件覆盖
        gc.setIdType(IdType.AUTO); // 主键策略   AUTO(0),自增  ASSIGN_UUID  分配UUID (主键类型为 string)
        gc.setServiceName("%sService");  // 设置生成的service接口的名字的首字母是否为I
        // IEmployeeService
        gc.setBaseResultMap(true);//生成基本的resultMap
        gc.setBaseColumnList(true);//生成基本的SQL片段
        gc.setOpen(false);// 是否打开输出目录 默认值:true
        gc.setSwagger2(true); //实体属性 Swagger2 注解
        mpg.setGlobalConfig(gc);
        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
//        dsc.setUrl("jdbc:mysql://www.xuerparking.cn:3306/project_lzx?useUnicode=true&useSSL=false&characterEncoding=utf8&noAccessToProcedureBodies=true&serverTimezone=GMT%2B8");
        dsc.setUrl("jdbc:mysql://121.41.131.154:3306/project_lzx?useUnicode=true&useSSL=false&characterEncoding=utf8&noAccessToProcedureBodies=true&serverTimezone=GMT%2B8");
        // dsc.setSchemaName("public");
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("123456");
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        // pc.setModuleName(scanner("模块名"));
        //包名
        pc.setParent("com.parkingmanage");
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // to do nothing
            }
        };
        // 策略配置

        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);//表名生成策略
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);// 数据库表映射到实体的命名策略
        // strategy.setSuperEntityClass("你自己的父类实体,没有就不用设置!");
        strategy.setEntityLombokModel(true);//Lombok【实体】是否为lombok模型（默认 false
        strategy.setRestControllerStyle(true);//开启生成@RestController 控制器
        // strategy.setTablePrefix("t_");//表前缀notifier_info
        strategy.setInclude(scanner("表名，多个英文逗号分割").split(","));
        strategy.setControllerMappingHyphenStyle(true);//驼峰转连字符
        strategy.setTablePrefix(pc.getModuleName() + "_");//是否生成实体时，生成字段注解
        //     strategy.setCapitalMode(true);			// 全局大写命名 ORACLE 注意
        mpg.setStrategy(strategy);
        //mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }
}

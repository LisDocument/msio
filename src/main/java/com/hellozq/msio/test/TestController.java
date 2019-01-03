package com.hellozq.msio.test;

import com.google.common.collect.Lists;
import com.hellozq.msio.anno.MsReturnTranslator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/us")
public class TestController {

    private static final Log log = LogFactory.getLog(TestController.class);

    @RequestMapping("/t")
    public Map test1(String title,String title2){
        return new HashMap(){
            {
                put("name","李");
                put("age","18");
            }
        };
    }


    @MsReturnTranslator(isComplex = true,id = "2")
    @RequestMapping("/tcomplex")
    public List test3(){
        HashMap hashMap = new HashMap();
        hashMap.put("name","李三思");
        hashMap.put("age","10");
        hashMap.put("hobby1","打球");
        hashMap.put("hobby2","不打球");
        return new ArrayList<Map>(){{
            add(hashMap);
        }};
    }


    @MsReturnTranslator(isComplex = true,id = "complexUser")
    @RequestMapping("/usercomplex")
    public List<ComplexUser> complexUser(){
        User user = new User();
        user.setAge(10);
        user.setName("李三思");
        User user1 = new User();
        user1.setName("李三思的小号");
        user1.setAge(12);
        FlexUser fuser = new FlexUser();
        fuser.setSex("男");
        fuser.setTelPhone("18658013201");
        fuser.setUser(user1);
        ComplexUser complexUser = new ComplexUser();
        complexUser.setUser(user);
        complexUser.setFlexUser(fuser);
        complexUser.setFlexUser1(fuser);
        //complexUser.setFlexUser2(fuser);
        List<ComplexUser> l = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            l.add(complexUser);
        }
        return l;
    }

    @MsReturnTranslator("getPage()#getList($int$5,$int$150,wowowo,$double$1.41)")
    @RequestMapping("/t2")
    public PageContent test2(){
        List list =  Lists.newArrayList();
        for (int i = 0; i < 360000; i++) {
            HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("name","李");
            objectObjectHashMap.put("age",i + "");
            list.add(objectObjectHashMap);
        }
        return new PageContent(new Page(list));
    }

    @RequestMapping("fil")
    public void ip(List<String> files){
        log.info("方法执行");

//        log.info(files.toString());
    }
}

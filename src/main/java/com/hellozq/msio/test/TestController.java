package com.hellozq.msio.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/us")
public class TestController {

    @RequestMapping("/t")
    public String test1(){
        return "xxxx";
    }

//    @RequestMapping("u/t")
//    public String test2(){
//        return "xxx2";
//    }
}

package com.Fanbbs.service.impl;

import com.Fanbbs.entity.*;
import com.Fanbbs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityServiceImpl  implements SecurityService {
    @Autowired
    private InboxService inboxService;
    @Autowired
    private UsersService usersService;

    @Override
    public void safetyMessage(String msg,String type){
        //向所有管理员发送警告
        try{
            Users user = new Users();
            user.setGroup("administrator");
            List<Users> userList = usersService.selectList(user);
            for (int i = 0; i < userList.size(); i++) {
                Inbox inbox = new Inbox();
                Integer uid = userList.get(i).getUid();
                Long date = System.currentTimeMillis();
                String created = String.valueOf(date).substring(0,10);
                Inbox insert = new Inbox();
                insert.setUid(uid);
                insert.setTouid(uid);
                insert.setType(type);
                insert.setText(msg);
                insert.setCreated(Integer.parseInt(created));
                inboxService.insert(insert);
            }
            System.err.println("有用户存在违规行为，已向所有管理员发送警告");
        }catch (Exception e){
            System.err.println(e);
        }
    }
}

package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/")
public class UserController {
    @Autowired
    private IUserService iUserService;

    /**
     * ResponseBody注解被应用于方法上，标志该方法的返回值应该被直接写回到HTTP响应体中去（而不会被被放置到Model中或被解释为一个视图名）
     * 在实际开发中，返回JSON 是最常见的一种方式，而SpringMVC提供了一种更为简便的方式输出JSON（非 JSP 页面），那就是使
     * ResponseBody 注解，可以将如下类型的数据做成json：
     * 1）基本数据类型，如 boolean , String , int 等
     * 2) Map 类型数据
     * 3）集合或数组
     * 4）实体对象
     * 5）实体对象集合
     * 依赖：jackson-core  jackson-annotations   jackson-databind
     * 原理：当一个处理请求的方法被标记为@ResponseBody时，就说明该方法需要输出其它视图（JSON、XML），SpringMVC 通过
     * 已定义的转化器做转化输出，默认输出 JSON
     * 区别：@RequestBody 是写在方法参数前，作用于方法参数；@ResponseBody 是写在方法上，作用于方法返回值
     */

    @RequestMapping(value = "login.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session) {
        ServerResponse<User> response = iUserService.login(username, password);
        // 这个response.isSuccess()判断逻辑是：1，这个response是service层传过来的一个返回值；
        // 2，response包含了一个code，要么是0，要么是1，如果是0，return this.status == ResponseCode.SUCCESS.getCode();就是true了
        if (response.isSuccess()) {
            // why：可是为什么页面能展示具体的详细信息呢？？？？？？？？？？？？？？？？？？
            session.setAttribute(Const.CURRENT_USER, response.getData());  //这里getData到的是service层传过来的user对象(password为"")
        }
        return response;
    }

    @RequestMapping(value = "logout.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session) {
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess(); // 存不存在登出失败？要不要加一些健壮性判断？？？？？？？？？？？？？？？？？
    }

    @RequestMapping(value = "register.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user) {
        return iUserService.register(user);
    }

    @RequestMapping(value = "check_Valid.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String input, String type) {
        return iUserService.checkValid(input, type);
    }

    @RequestMapping(value = "get_user_info.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session) {
        // 通过和前端的约定，获取session里保存的用户信息？？？？？？？？？？？？？
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user != null) {
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登陆，无法获取当前用户信息");
    }

    @RequestMapping(value = "forget_get_question.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username) {
        return iUserService.selectQuestion(username);
    }

    @RequestMapping(value = "forget_check_answer.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        return iUserService.checkAnswer(username, question, answer);
    }

    @RequestMapping(value = "forget_reset_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        return iUserService.forgetResetPassword(username, passwordNew, forgetToken);
    }

    @RequestMapping(value = "reset_password.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpSession session, String passwordOld, String passwordNew) {
        // 登陆状态下该密码，user从前端传来
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户未登陆");
        }
        return iUserService.resetPassword(passwordOld, passwordNew, user);
    }

    @RequestMapping(value = "update_information.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> updateInformation(HttpSession session, User user) {
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户未登陆");
        }
        user.setId(currentUser.getId());
        user.setUsername(user.getUsername());
        ServerResponse<User> response = iUserService.updateInformation(user);
        if (response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }
        return response;
    }

    @RequestMapping(value = "get_information.do", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getInformation(HttpSession session) {
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "未登陆，需要强制登陆status=10");
        }
        return iUserService.getInformation(currentUser.getId());
    }
}
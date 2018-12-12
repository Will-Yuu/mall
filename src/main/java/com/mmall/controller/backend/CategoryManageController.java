package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/category/")
public class CategoryManageController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private ICategoryService iCategoryService;

    @RequestMapping("add_category.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session, String categoryName, @RequestParam(value = "parentId", defaultValue = "0") int parentId) {
        // 先从session域获取当前user对象
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        // 判断非空
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆，请先登陆");
        }
        // 判断是否为管理员账户类型
        if (iUserService.checkAdminRole(user).isSuccess()) {
            return iCategoryService.addCategory(categoryName, parentId);
        }
        return ServerResponse.createByErrorMessage("没有操作权限，需要管理员权限");
    }

    @RequestMapping("update_categoryName.do")
    @ResponseBody
    public ServerResponse updateCategoryName(HttpSession session, Integer categoryId, String categoryName) {
        // 先从session域获取当前user对象
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        // 判断非空
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆，请先登陆");
        }
        // 判断是否为管理员账户类型
        if (iUserService.checkAdminRole(user).isSuccess()) {
            return iCategoryService.updateCategoryName(categoryId, categoryName);
        }
        return ServerResponse.createByErrorMessage("没有操作权限，需要管理员权限");
    }

    // 根据传入parentId(如果没传入默认0)查询当前parentId的商品
    @RequestMapping("get_categoryByParentId.do")
    @ResponseBody
    public ServerResponse getCategoryByParentId(HttpSession session, @RequestParam(value = "parentId", defaultValue = "0") Integer parentId) {
        // 先从session域获取当前user对象
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        // 判断非空
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆，请先登陆");
        }
        // 判断是否为管理员账户类型
        if (iUserService.checkAdminRole(user).isSuccess()) {
            // 根据传入的根据传入parentId查询
            return iCategoryService.getCategoryByParentId(parentId);
        }
        return ServerResponse.createByErrorMessage("没有操作权限，需要管理员权限");
    }

    @RequestMapping("get_categoryAndChildrenByParentId.do")
    @ResponseBody
    public ServerResponse getCategoryAndChildrenByParentId(HttpSession session, @RequestParam(value = "parentId", defaultValue = "0") Integer parentId) {
        // 先从session域获取当前user对象
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        // 判断非空
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆，请先登陆");
        }
        // 判断是否为管理员账户类型
        if (iUserService.checkAdminRole(user).isSuccess()) {
            return iCategoryService.getCategoryAndChildrenByParentId(parentId);
        }
        return ServerResponse.createByErrorMessage("没有操作权限，需要管理员权限");
    }
}

package com.al.ddpunch;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.al.ddpunch.util.CMDUtil;
import com.al.ddpunch.util.LogUtil;
import com.al.ddpunch.util.SharpData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangxiaoming on 2018/7/25.
 * 模拟点击主服务
 */

public class MainAccessService extends AccessibilityService {


    //考勤页面判定
    public static String webview_page_ResId = "com.alibaba.android.rimet:id/webview_frame";

    //主页
    public static String main_page_ResId = "com.alibaba.android.rimet:id/home_bottom_tab_root";

    public static String work_page_ResId = "com.alibaba.android.rimet:id/home_bottom_tab_button_work";

    //工作页判定
    public static String kaoqin_page_ResId = "com.alibaba.android.rimet:id/oa_fragment_gridview";


    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {


        if (SharpData.getOpenApp(getApplicationContext()) == 1) {
            LogUtil.E("工作停止,请手动开启工作");
            return;
        }
        int iscom = SharpData.getIsCompent(getApplicationContext());
        int order = SharpData.getOrderType(getApplicationContext());

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (order == 0) {
                LogUtil.E("当前无任务");
                return;
            }
            LogUtil.D("指令-->" + order);
            if (iscom == 0 || iscom != order) {
                new_work(order);
            } else {
                LogUtil.D("已打卡-->" + order);
            }


        }


    }

    private void new_work(int order) {



        try {
            //脚本初始化,判断是否在主页
            AccessibilityNodeInfo node = refshPage();
            if (node == null || !Comm.launcher_PakeName.equals(node.getPackageName().toString())) {
                throw new Exception("程序不在初始化启动器页面,抛出异常");
            }
            int m = 10;
            while (m > 0) {
                LogUtil.D("循环--" + node);
                if (node != null && Comm.dingding_PakeName.equals(node.getPackageName().toString())) {
                    node = refshPage();
                    LogUtil.D("已进入app" + node);

                    //进入主页
                    break;
                } else {
                    startApplication(getApplicationContext(), Comm.dingding_PakeName);
                }
                sleepT(1000);  //1秒钟启动一次
                if (node != null) {
                    node = refshPage();
                }
                m--;
            }
            if (m <= 0) {
                throw new Exception("启动钉钉异常");
            }
            sleepT(1000);

            int k = 10;
            while (k > 0) {
                //确认进入钉钉主页
                if (findResIdById(node, main_page_ResId)) {
                    LogUtil.D("已进入app主页");
                    break;
                }
                sleepT(1000);
                if (node != null)
                    node = refshPage();
                k--;
            }
            if (k <= 0) {
                throw new Exception("已进入app,未找到主页节点");
            }


            //进入工作页,点击工作按钮
            if (!findResIdById(node, work_page_ResId)) {
                throw new Exception("已进入主页,未找到工作页按钮节点");
            } else {
                List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByViewId(work_page_ResId);
                list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            sleepT(1000);
            node = refshPage();

            //确认进入工作页,点击考勤打卡节点,进入考勤页面

            List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByViewId(kaoqin_page_ResId);
            if (list != null || list.size() != 0) {
                node = list.get(0);
                if (node != null || node.getChildCount() >= 8) {
                    node = node.getChild(7);
                    if (node != null) {  //已找到考勤打卡所在节点,进行点击操作
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    } else {
                        throw new Exception("已进入工作页,但未找到考勤打卡节点");
                    }
                } else {
                    throw new Exception("已进入工作页,但未找到考勤打卡节点");
                }
            } else {
                throw new Exception("已进入工作页,但未找到相关节点");
            }

            node = refshPage();

            int l=10;
            while (l>0){

                List<AccessibilityNodeInfo> list2 = node.findAccessibilityNodeInfosByViewId(webview_page_ResId);
                if (list2 != null || list2.size() != 0) {
                    break;
                }
                l--;
                node=refshPage();
                sleepT(1000);
            }

            if(l<=0){
                throw new Exception("进入考勤打卡页面异常");
            }
            LogUtil.D("确认已进入考勤打卡页面");

            //尝试打卡操作
            int j = 2;
            while (j >= 0) {
                LogUtil.D("尝试打卡操作->" + j);
                DoDaKa(order);
                //执行完操作之后,判断是否打卡成功
                sleepT(2000);
                j--;
            }

            if (node != null) node.recycle();
        } catch (Exception e)

        {
            LogUtil.E(e.getMessage());

            //执行回退操作
            AppCallBack();
        }

    }


    private void DoDaKa(int order) {
        if (order == 1) {
            //上班打卡
            CMDUtil.ClickXy("240", "314");
        } else if (order == 2) {
            //下班卡
            CMDUtil.ClickXy("262", "556");
        }

        //检查是否打卡成功
//       ;
//        LogUtil.D("根节点");
//        //查询所有的根节点,假如有弹窗,说明打卡成功
//        List<AccessibilityNodeInfo> list = getAllNode(node, null);
//        LogUtil.D("所有节点个数-->" + list.size());
//        if (list != null) {
//            for (AccessibilityNodeInfo info : list) {
//                String className = info.getClassName().toString();
//                if ("android.app.Dialog".equals(className)) {
//                    //说明可能是打卡导致的成功弹窗
//                    AccessibilityNodeInfo nodeInfo = info.getChild(0);
//                    if (nodeInfo != null) {
//                        nodeInfo = nodeInfo.getChild(1);
//                        if (nodeInfo != null) {
//                            String des = nodeInfo.getContentDescription().toString();
//                            if (des.contains("打卡成功")) {
//                                return true;
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//        return false;
    }

    //程序异常时的操作方法
    private void AppCallBack() {
        int i = 10;
        while (true) {
            //执行回退操作
            AccessibilityNodeInfo node = refshPage();

            if (i < 0) {
                //说明可能卡住了,无法回退,强行停止程序进程
                if (node != null) {
                    CMDUtil.stopProcess(node.getPackageName().toString());

                } else {
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);

                }
                break;
            }
            LogUtil.D("执行回退操作");
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            if (node != null && Comm.launcher_PakeName.equals(node.getPackageName().toString())) {
                //已回退到启动页,退出循环
                LogUtil.D("已回到初始页");
                break;
            }
            i--;
            sleepT(2000); //睡眠一秒
        }

    }


    //刷新节点
    private AccessibilityNodeInfo refshPage() {
        return getRootInActiveWindow();
    }

    //递归获取所有节点
    private List<AccessibilityNodeInfo> getAllNode(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        LogUtil.D("递归节点-->"+node+"---"+node.getChildCount());
        if (node != null && node.getChildCount() != 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo info = node.getChild(i);
                if (info != null) {
                    LogUtil.D("打卡节点数-"+info);
                    list.add(info);
                    node = info;

                }
            }

        } else {
            return list;
        }

        return getAllNode(node, list);

    }


    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

    }


    public static boolean startApplication(Context context, String packageName) {
        boolean rlt = false;

        PackageManager pkgMgr = context.getPackageManager();
        if (null != pkgMgr) {
            Intent intent = pkgMgr.getLaunchIntentForPackage(packageName);
            if (null != intent) {
                context.startActivity(intent);
                rlt = true;
            }
        }
        return rlt;
    }


    public boolean findResIdById(AccessibilityNodeInfo info, String resId) {
        if (info == null) {
            return false;
        }
        List<AccessibilityNodeInfo> list = info.findAccessibilityNodeInfosByViewId(resId);

        if (list == null || list.size() == 0) {
            return false;
        }
        return true;
    }


    public void sleepT(long t) {

        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
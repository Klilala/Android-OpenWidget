package com.klilala.openwidget.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Scroller
import kotlin.math.abs

/**
 * @author      : ZQ
 * date         : 2025/2/20 9:15
 * package_name : com.klilala.openwidget.widget
 * desc         : 跟随手指滑动出侧边栏
 * 使用方法：
 *               <LinearLayout
 *             android:layout_width="@dimen/dp_300"
 *             android:layout_height="match_parent"
 *             android:background="@color/beauty_color_red"
 *             android:orientation="vertical">
 *             <TextView
 *                 android:id="@+id/tv_left"
 *                 android:layout_width="match_parent"
 *                 android:layout_height="wrap_content"
 *                 android:text="左侧页面12321312"/>
 *         </LinearLayout>
 *
 *         <include layout="@layout/fragment_kli_ai"/>
 *     </com.klilala.lib_kai.widget.SlideMenu>
 *
 *     子组件第一个必须是左侧页面，第二个才是主页面
 */
class SlideMenu @JvmOverloads constructor (context: Context, attributeSet: AttributeSet?) :
    FrameLayout(context, attributeSet) {
    //按下的x坐标
    private var downX: Float = 0f
    //按下的y坐标
    private var downY: Float = 0f
    private var moveX: Float = 0f
    var MAIN_STATE: Int = 0
    var MENU_STATE: Int = 1
    //当前模式
    private var currentState = MAIN_STATE
    private var scroller: Scroller

    init {
        scroller = Scroller(context)
    }

    private var leftMenuWidth: Int = 0

    //左侧页面
    private lateinit var leftMenuView: View
    private lateinit var mainPageView: View

    //是否给主页面添加遮罩层
    private var isAddMask = false
    private var listener: AddMaskListener? = null

    /**
     * 测量并设置 所有子View的宽高 widthMeasureSpec: 当前控件的宽度测量规则 heightMeasureSpec:
     * 当前控件的高度测量规则
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //指定左面板的宽高
        leftMenuView = getChildAt(0)

        leftMenuWidth = leftMenuView.layoutParams.width

        leftMenuView.measure(leftMenuWidth, heightMeasureSpec)

        //指定主面板的宽高
        mainPageView = getChildAt(1)

        mainPageView.measure(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * changed: 当前控件的尺寸大小, 位置 是否发生了变化 left:当前控件 左边距 top:当前控件 顶边距 right:当前控件 右边界
     * bottom:当前控件 下边界
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        leftMenuView.layout(-leftMenuWidth, 0, 0, b)//
        mainPageView.layout(l, t, r, b)
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                //按下
                downX = event.x
            }

            MotionEvent.ACTION_MOVE -> {
                //按下时的移动
                moveX = event.x
                //将要发生的偏移量
                val scrollX = (downX - moveX).toInt()

                val newScrollPosition = getScrollX() + scrollX
                //限定左边
                if(newScrollPosition < -leftMenuWidth){
                    scrollTo(-leftMenuWidth, 0)//滚动到指定位置
                }else if(newScrollPosition > 0){
                    //限定右边
                    scrollTo(0, 0)
                } else{
                    //让偏移量生效
                    listener?.addMask()
                    scrollBy(scrollX, 0)//基于当前位置滚动
                }
                if(!isAddMask){
                    //添加遮罩

                    isAddMask = true
                }
                downX = moveX
            }

            MotionEvent.ACTION_UP ->{
                //抬起手
                val location = IntArray(2)
                leftMenuView.getLocationOnScreen(location)
                val screenX = location[0]

                if(screenX == 0){
                    //打开了侧边栏
                    currentState = MENU_STATE
                }else if(screenX.toFloat() == leftMenuView.x){
                    //关闭了侧边栏
                    currentState = MAIN_STATE
                    listener?.closeMask()
                }else{
                    //轻扫页面，靠惯性做打开侧边或关闭侧边
                    if((abs(scrollX) >= leftMenuWidth/3 && abs(scrollX) != leftMenuWidth)){
                        //自动展开侧边
                        currentState = MENU_STATE
                    }else if(scrollX != 0 && abs(scrollX) < leftMenuWidth/3){
                        //关闭侧边
                        currentState = MAIN_STATE
                        listener?.closeMask()
                    }
                }
                updateCurrentContent()
            }

        }
        return true
    }

    /**
     * 根据当前的状态, 执行 关闭/开启 的动画
     */
    private fun updateCurrentContent(isOpen: Boolean = false, isClose: Boolean = false) {
        var startX = scrollX
        var dx = 0
        //平滑滚动
        if(currentState == MENU_STATE){
//            dx = -getChildAt(0).measuredWidth
            dx = -leftMenuWidth
        }else{
            dx = 0 - startX
        }

        // startX: 开始的x值
        // startY: 开始的y值
        // dx: 将要发生的水平变化量. 移动的x距离
        // dy: 将要发生的竖直变化量. 移动的y距离
        // duration : 数据模拟持续的时长

        //1.开始平滑的数据模拟 拖到一定位置，执行这里
        if(currentState == MENU_STATE || isOpen){
            //移动到左侧页面宽度的一半，那就动画展开整个左侧页面
            scroller.startScroll(startX, 0, -leftMenuWidth+ abs(startX), 0, 300)
        }else if(currentState == MAIN_STATE || isClose){
            //还原
            scroller.startScroll(startX, 0, dx, 0, 300)
        }

        //重绘节目 -> drawChild() -> computeScroll()
        invalidate()
    }

    //2. 维持动画的继续
    override fun computeScroll() {
        super.computeScroll()
        // 直到duration事件以后, 结束
        if(scroller.computeScrollOffset()){
            // true, 动画还没有结束
            // 获取当前模拟的数据, 也就是要滚动到的位置
            val currX = scroller.currX
            //滚动过去
            scrollTo(currX, 0)
            invalidate()
        }
    }

    fun open() {
        currentState = MENU_STATE
        listener?.addMask()
        updateCurrentContent(isOpen = true)
    }

    fun close(){
        currentState = MAIN_STATE
        listener?.closeMask()
        updateCurrentContent(isClose = true)
    }

    fun switchState(){
        if(currentState == MAIN_STATE){
            open()
        }else{
            close()
        }
    }

    fun getCurrentState() = currentState

    //蒙层添加监听
    fun addMaskListener(listener: AddMaskListener){
        this.listener = listener
    }


    /**
     * 拦截判断
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when(ev?.action){
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val xOffset = abs(ev.x - downX)
                val yOffset = abs(ev.y - downY)

                //水平方向超出一定距离时,才拦截
                if(xOffset > yOffset && xOffset > 5){
                    return true // 拦截此次触摸事件, 界面的滚动
                }
            }

            MotionEvent.ACTION_UP -> {}
        }
        return super.onInterceptTouchEvent(ev)
    }
}

interface AddMaskListener{
    fun addMask()

    fun closeMask()
}
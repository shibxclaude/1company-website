$(function() {
	
	/* 导航效果 */
	$(function(){
	    $(".mobNav").click(function() {
	    	$(".mobMenu").css({
	            "left": "0",
	            "transition": "0.5s"
	      });
    	});
    	$(".closeIcon").click(function() {
    		$(".mobMenu").animate({
	            "left": "100%",
	            "transition": "0s"
	        }, 500);
           
	    });
	   
	    
});
    
	/*banner轮播*/
	$(function(){
        $(".owl-demo").owlCarousel({
            items: 1,
            navigation: true,
            navigationText: ["上一个","下一个"],
            autoPlay: true,
            stopOnHover: true
        }).hover(function(){
            $(".owl-buttons").show();
        }, function(){
            $(".owl-buttons").hide();
        });
    });
    
    /* 首页点击滚动 */
     $(function() {
        var blw=$(".myScrollbox li").width();
        //获取单个子元素所需宽度
        var liArr = $(".myScrollbox ul").children("li");
        //获取子元素数量
        var mysw = $(".myScroll").width();
        //获取子元素所在区域宽度
        var mus = parseInt(mysw/blw);
        //计算出需要显示的子元素的数量
        var length = liArr.length-mus;
        //计算子元素可移动次数（被隐藏的子元素数量）
        var i=0
        $(".right").click(function(){
            i++
            //点击i加1
            if(i<length){
                $(".myScrollbox").css("left",-(blw*i));
                //子元素集合向左移动，距离为子元素的宽度乘以i。
            }else{
                i=length;
                $(".myScrollbox").css("left",-(blw*length));
                //超出可移动范围后点击不再移动。最后几个隐藏的元素显示时i数值固定位已经移走的子元素数量。
            }
        });
        $(".left").click(function(){
            i--
            //点击i减1
            if(i>=0){
                $(".myScrollbox").css("left",-(blw*i));
                //子元素集合向右移动，距离为子元素的宽度乘以i。
            }else{
                i=0;
                $(".myScrollbox").css("left",0);
                //超出可移动范围后点击不再移动。最前几个子元素被显示时i为0。
            }
        });
    });
	
	
   
    /* 招聘 */
    $(".tableBody").find("a.expand").click(function() {
            if ($(this).parent().parent().next(".aboutProcess").css("display") == "none") {
            	$(".expand").children("i").addClass("downIcon").removeClass("upIcon");
                $(this).children("i").addClass("upIcon").removeClass("downIcon");
                $(".expand").children("span").text("展开");
                $(this).children("span").text("收起");
                $(".aboutProcess").hide();
                $(this).parent().parent().next().slideDown();
                $(this).parent().parent().next().css("background","#f9f9f9");
            } else {
                $(this).children("span").text("展开");
                $(this).children("i").addClass("downIcon").removeClass("upIcon");
                $(this).parent().parent().next().hide();
            }

    });
});        
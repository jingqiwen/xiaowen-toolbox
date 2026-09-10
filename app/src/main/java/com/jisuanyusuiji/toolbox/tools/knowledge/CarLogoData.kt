package com.jisuanyusuiji.toolbox.tools.knowledge

data class CarLogo(val name: String, val badge: String)

/** 车标图鉴：只展示车标图标（文字徽标）与品牌名称。 */
object CarLogoData {
    val items = listOf(
        CarLogo("大众", "VW"), CarLogo("丰田", "T"), CarLogo("本田", "H"),
        CarLogo("日产", "N"), CarLogo("福特", "Ford"), CarLogo("雪佛兰", "+"),
        CarLogo("别克", "Buick"), CarLogo("凯迪拉克", "Cad"), CarLogo("GMC", "GMC"),
        CarLogo("Jeep", "Jeep"), CarLogo("道奇", "Dodge"), CarLogo("特斯拉", "T"),
        CarLogo("奔驰", "Benz"), CarLogo("宝马", "BMW"), CarLogo("奥迪", "Audi"),
        CarLogo("保时捷", "P"), CarLogo("法拉利", "SF"), CarLogo("兰博基尼", "LB"),
        CarLogo("玛莎拉蒂", "M"), CarLogo("宾利", "B"), CarLogo("劳斯莱斯", "RR"),
        CarLogo("迈凯伦", "McL"), CarLogo("阿斯顿·马丁", "AM"), CarLogo("路虎", "LR"),
        CarLogo("捷豹", "J"), CarLogo("沃尔沃", "Volvo"), CarLogo("雷克萨斯", "L"),
        CarLogo("英菲尼迪", "INF"), CarLogo("讴歌", "A"), CarLogo("现代", "H"),
        CarLogo("起亚", "Kia"), CarLogo("斯巴鲁", "Sub"), CarLogo("马自达", "Mazda"),
        CarLogo("三菱", "Mits"), CarLogo("标致", "PEU"), CarLogo("雪铁龙", "CIT"),
        CarLogo("雷诺", "R"), CarLogo("菲亚特", "Fiat"), CarLogo("阿尔法·罗密欧", "Alfa"),
        CarLogo("斯柯达", "Skoda"), CarLogo("比亚迪", "BYD"), CarLogo("吉利", "Geely"),
        CarLogo("长城", "GWM"), CarLogo("奇瑞", "Chery"), CarLogo("长安", "CHAN"),
        CarLogo("红旗", "HQ"), CarLogo("蔚来", "NIO"), CarLogo("小鹏", "XP"),
        CarLogo("理想", "Li"), CarLogo("领克", "Lynk")
    )
}

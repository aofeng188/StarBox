import Foundation

enum GeneratorData {
    /// 中国传统色（常见色名与近似色值）
    static let traditionalColors: [NamedColor] = [
        .init(name: "胭脂", hex: 0x9D2933), .init(name: "朱砂", hex: 0xFF461F), .init(name: "绯红", hex: 0xC83C23), .init(name: "海棠红", hex: 0xDB5A6B),
        .init(name: "桃红", hex: 0xF47983), .init(name: "嫣红", hex: 0xEF7A82), .init(name: "银红", hex: 0xF05654), .init(name: "酡红", hex: 0xDC3023),
        .init(name: "品红", hex: 0xF00056), .init(name: "绛紫", hex: 0x8C4356), .init(name: "藕荷", hex: 0xE4C6D0), .init(name: "丁香", hex: 0xCCA4E3),
        .init(name: "雪青", hex: 0xB0A4E3), .init(name: "青莲", hex: 0x801DAE), .init(name: "黛紫", hex: 0x574266), .init(name: "藏青", hex: 0x2E4E7E),
        .init(name: "靛蓝", hex: 0x065279), .init(name: "宝蓝", hex: 0x4B5CC4), .init(name: "群青", hex: 0x4C8DAE), .init(name: "石青", hex: 0x1685A9),
        .init(name: "湖蓝", hex: 0x30DFF3), .init(name: "月白", hex: 0xD6ECF0), .init(name: "蟹壳青", hex: 0xBBCDC5), .init(name: "鸦青", hex: 0x424C50),
        .init(name: "墨色", hex: 0x50616D), .init(name: "黛绿", hex: 0x426666), .init(name: "苍翠", hex: 0x519A73), .init(name: "竹青", hex: 0x789262),
        .init(name: "葱绿", hex: 0x0EB83A), .init(name: "松花绿", hex: 0xBCE672), .init(name: "碧色", hex: 0x1BD1A5), .init(name: "翡翠", hex: 0x3DE1AD),
        .init(name: "青碧", hex: 0x48C0A3), .init(name: "茶白", hex: 0xF3F9F1), .init(name: "象牙白", hex: 0xFFFBF0), .init(name: "缟色", hex: 0xF2ECDE),
        .init(name: "鹅黄", hex: 0xFFF143), .init(name: "秋香", hex: 0xD9B611), .init(name: "姜黄", hex: 0xFFC773), .init(name: "杏黄", hex: 0xFFA631),
        .init(name: "橘黄", hex: 0xFF8936), .init(name: "琥珀", hex: 0xCA6924), .init(name: "赭石", hex: 0x845A33), .init(name: "驼色", hex: 0xA88462),
        .init(name: "檀色", hex: 0xB36D61), .init(name: "珊瑚", hex: 0xFF7F50), .init(name: "霁青", hex: 0x63BBD0), .init(name: "天水碧", hex: 0x5AA4AE),
    ]

    static let surnames = Array("王李张刘陈杨黄赵吴周徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾萧田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤").map(String.init)
    static let compoundSurnames = ["欧阳", "司马", "上官", "诸葛", "慕容", "南宫", "东方", "令狐"]
    static let maleChars = Array("宇轩浩然子墨梓睿泽辰逸昊天佑铭博文瑞霖承安景行致远思源嘉懿煜城懿轩烨伟鸿涛晨阳俊驰皓明晟睿哲瀚彦卓凯昕旭尧舟川").map(String.init)
    static let femaleChars = Array("梓涵欣怡诗雨语嫣若曦可馨梦琪雅静思彤一诺芷若清欢婉清悦心沐晴紫萱嘉怡安然星辰知夏初夏书瑶念慈语桐晓棠").map(String.init)
    static let neutralChars = Array("安宁可言知行一白予夏清和星河云舒南风若水思远沐阳").map(String.init)

    static let englishMale = ["Leo", "Oliver", "Ethan", "Noah", "Lucas", "Mason", "Elijah", "Logan", "Aiden", "Caleb", "Owen", "Ryan", "Nathan", "Dylan", "Ian", "Jasper", "Felix", "Hugo", "Theo", "Milo", "Adrian", "Julian", "Kai", "Max", "Sean", "Victor", "Eric", "Kevin", "Jason", "Allen"]
    static let englishFemale = ["Luna", "Olivia", "Emma", "Ava", "Mia", "Chloe", "Lily", "Grace", "Zoe", "Ivy", "Nora", "Ella", "Aria", "Stella", "Hazel", "Iris", "Ruby", "Cora", "Alice", "Elsa", "Sophie", "Clara", "Vivian", "Hannah", "Jessie", "Amy", "Cindy", "Daisy", "Fiona", "Yuki"]

    static let nickAdjectives = ["温柔的", "爱吃饭的", "不想上班的", "会发光的", "迷路的", "快乐的", "失眠的", "认真的", "糊涂的", "勇敢的", "慢吞吞的", "元气满满的", "安静的", "闪闪发光的", "想去旅行的", "爱睡觉的", "热爱生活的", "在逃的", "可爱的", "清醒的"]
    static let nickNouns = ["小熊", "月亮", "橘子汽水", "星星", "猫咪", "柠檬", "云朵", "海盐", "小鲸鱼", "蘑菇", "奶盖", "晚风", "兔子", "芝士", "企鹅", "西瓜", "小王子", "棉花糖", "宇航员", "气泡"]
    static let petNames = ["团子", "豆包", "可乐", "布丁", "年糕", "汤圆", "奶糖", "芝麻", "饭团", "毛毛", "球球", "皮蛋", "旺财", "来福", "咪咪", "胖虎", "花卷", "麻薯", "雪球", "栗子"]
}

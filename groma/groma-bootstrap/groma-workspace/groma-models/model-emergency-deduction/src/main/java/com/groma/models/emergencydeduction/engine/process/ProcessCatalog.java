package com.groma.models.emergencydeduction.engine.process;

import com.groma.base.engine.BaseSimEntity;
import com.groma.base.engine.GromaMemberMeta;
import com.groma.base.engine.GromaModuleRuntime;
import com.groma.base.engine.GromaProcessTask;
import com.groma.base.engine.GromaValues;
import com.groma.base.engine.GromaWorldContext;
import com.groma.models.emergencydeduction.model.entity.AreaEntity;
import com.groma.models.emergencydeduction.model.entity.FileInfoEntity;
import com.groma.models.emergencydeduction.model.entity.FileServiceEntity;
import com.groma.models.emergencydeduction.model.entity.FileTypeEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithStationEntity;
import com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.MetroSectionEntity;
import com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity;
import com.groma.models.emergencydeduction.model.entity.PlanExecutorEntity;
import com.groma.models.emergencydeduction.model.entity.PlanGeneratorEntity;
import com.groma.models.emergencydeduction.model.entity.RainfallEntity;
import com.groma.models.emergencydeduction.model.entity.Scene1Entity;
import com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity;
import com.groma.models.emergencydeduction.model.entity.StationEntity;
import com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity;
import com.groma.models.emergencydeduction.model.entity.TrainEntity;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.util.List;
import java.util.Map;

/**
 * 过程目录：按过程定义键新建过程任务；形参以中文原名传值（外部干预、MCP 无会话执行、存档续跑共用）
 */
public final class ProcessCatalog {
    public static final List<GromaModuleRuntime.ProcessDescriptor> PROCESSES = List.of(new GromaModuleRuntime.ProcessDescriptor("dv:designView_04fb48efd78444e3bfc948e0fa91c1f3", "ae6ce890734549a29aa10b5b358bab99", "判断文件是否存在", "FileType", "文件类型", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_0joI684F5zzw3QGvIDY", "zlRqcFoeLHuEKONcV2V", "入口过水", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_175Y0QSMw786DGgXBXZ", "E5B8xzaV9xRnt2LDEgH", "积水变化", "Area", "区域", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_1BqELDRmZFoiytzNKQ5", "Une96ICnH2KER5gu7LT", "地表排水", "Area", "区域", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_1tvmsrr8ydvpCLGAoxr", "7rSlUlS0cSWjxGaiErg", "沿区间前进", "Train", "列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_218a29d6cd004955b234ad6b1bafc7b6", "4da6a71b5a2943d887fd9249ef48d05c", "判断是否为空文件", "FileType", "文件类型", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_2WLvrJyBv6htmVscnRk", "ctET0FcFZxktbhbbd9j", "2把否变成是", "Scene1", "场景1", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_2aI60V2gYdhoWiHC5B3", "a9Ho9Zh9bCnkV8wVlfJ", "水流交换间隔", "Station", "车站", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_3xY5i4HkUNciNe3ll79", "JzfewV01yPJs3e4QdtE", "获取雨量序列", "Rainfall", "降雨", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_5mX6EkRtocaf9rXbKu1", "HOI0UpBGLHJoNJjKYum", "车站与区间水流交换", "MetroSection", "地铁区间", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_659efee68fd74a0a86fa968e044a59aa", "bf6717304c85425eb4e697d24ab6d95a", "文件初始化", "FileType", "文件类型", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_65ClRS3n9vUb3935COH", "oHh6xrqSMTxFFZGd07W", "查找前方车站", "", "独立过程", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_767z0mfmOaNst2dPwlG", "wjMiAj779lB5502MLmW", "离站", "Train", "列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_80233ba35326427aa7886a6ec6051604", "615b216a7eba490a90e986eeae52eea9", "读取文本内容", "FileType", "文件类型", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_BPlSUsxYsStEoFQEAGD", "Pupqb6kGlHmKlcG6aaU", "确定两端水位", "", "独立过程", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_DwYZrRL0fsJBQ7H79np", "d3UYEBS88YPu8FoGckq", "水深变化", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_FVDIGUCMq2RHeXb9Wgr", "6qnqt4O6vaFhEkh96tN", "水流交换间隔", "MetroSection", "地铁区间", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_H4eBTelUToW12IqLJ8O", "TUmeBFJE9XAot7jIGeV", "确定连接处过水情况", "", "独立过程", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_JTcC0L3UTKJxpFJ1ods", "FUrqUDTrCHxy0I97epN", "获取本步时长", "", "独立过程", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_P80bc3nl6S9bWISn6bg", "IJT778zU1zIw6nc7uYe", "计算处置时间", "PlanGenerator", "方案生成器", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_XLrKPtiJlmUbySIChuF", "u9xyHBv1BSldBPVP0Q7", "区间行驶", "Train", "列车", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_aroqnVQzhKVnM1h7wPt", "QZPPbXby5BPfgS5sziq", "计算降雨汇水", "Area", "区域", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_bXHqX2hJtSvKqqleN8E", "Sq99s0zjy6vmnjVSRMz", "区间水回流车站", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_eHq1YpIkoLxwHKywWis", "1rbne3IC1GpVoTNMwVu", "停站", "Train", "列车", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_fUFQ4qvShQtFqOghp0w", "KPVmSbBvALXOLKuleXT", "站内水回流地面", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_fjlFgY29HmzqjbmHxYC", "TvGbpspuGv5vJ1zt0qX", "降雨", "Rainfall", "降雨", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_i8XnCQtmJLFiB3KzdpL", "uK01gkyWkOsTbw8coIE", "积水变化", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_joMlLooi2InkPOjk59H", "zkevt3isbPbfdXv9T9I", "进站", "Train", "列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_kVXT3R1XGHTwkie2rur", "RDFt6UgNzfxWfBN4FsT", "积水变化", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_kclxVkgMIrkBe36K656", "QzcUew3b1XLCXLK6rIH", "排水", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_l5HF6zfm23Aq3LAjrs5", "CfcyX4WesZ6Y5r66Caw", "车站水流入区间", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_miDwXHXHR407mRDMPzO", "llOioPfUfaWKZf5nX3v", "4生成对象并把是变成否", "Scene1", "场景1", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_rXSwECDiXYGPqX12Bir", "6gimbvYC5CskuHCzVFQ", "挡水墙承受水压", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_sJx0EiISfyfjakZXrm0", "ZBZhSMCGmdAeAUO90BY", "3转换为行驶", "Scene1", "场景1", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_tOSCSMGoda2sHYuuDqN", "eJ4DHDmPNpDiyA8MPzb", "形成车站与列车的关联", "LineWithTrain", "线路与列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_vFHwnod6GqHo3Bde4P5", "hzatYjpxW7rcjt42TK2", "1转换为停站", "Scene1", "场景1", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_xmjSEU4SGv2otlq6mLP", "mXoawsnL7qgqcwNmvSw", "区域与站内水流交换", "Station", "车站", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_yYvJXg9UMsYaNFITgoc", "d890lEbZtQNlk7z52Xa", "地面水流入车站", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("dv:designView_z02AJALkr13QaoAQd4l", "Ls7rMbkit4rP36FTZiY", "形成区间与列车的关联", "LineWithTrain", "线路与列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:3UULlXsUgqgKXOiinlG", "3UULlXsUgqgKXOiinlG", "读取文本内容", "FileService", "文件服务", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:3uWavwYkJfsqvNp2BuZ", "3uWavwYkJfsqvNp2BuZ", "过程1", "Rainfall", "降雨", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:9W9vzQHklaUg4QG70Vc", "9W9vzQHklaUg4QG70Vc", "转换为积水状态", "Area", "区域", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:9gvX0IJx27OFUEQmGbX", "9gvX0IJx27OFUEQmGbX", "创建关联", "LineWithTrain", "线路与列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:AqYEeNHBfq4AjItqD8C", "AqYEeNHBfq4AjItqD8C", "区间无积水", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:FylD7oTkdptLzbz6ZeT", "FylD7oTkdptLzbz6ZeT", "挡水墙失效", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:IH42eYNsj0haPC4ctyQ", "IH42eYNsj0haPC4ctyQ", "车站有积水", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:IeK32FL7HMYxvSd4PnP", "IeK32FL7HMYxvSd4PnP", "判断文件是否为空", "FileService", "文件服务", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:NdLStqTVimDD7Npig19", "NdLStqTVimDD7Npig19", "转换为无积水状态", "Area", "区域", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:Ne04mUfbnKpBEjTlARK", "Ne04mUfbnKpBEjTlARK", "文件初始化", "FileService", "文件服务", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:QQMUU7DuY51tlC5lIlP", "QQMUU7DuY51tlC5lIlP", "过程1", "Scene1", "场景1", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("ent:R4yssk5cauVoZXGtqkM", "R4yssk5cauVoZXGtqkM", "逐行读取文件内容", "FileService", "文件服务", "耗时过程", true, ""), new GromaModuleRuntime.ProcessDescriptor("ent:UkKv7XkK4ASpaf1Bnit", "UkKv7XkK4ASpaf1Bnit", "形成备选响应方案", "PlanGenerator", "方案生成器", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:XUr2ph0kb7en26PZ8gp", "XUr2ph0kb7en26PZ8gp", "记录危险暴露", "PassengerGroup", "乘客群", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:azSevtqA1GFz9T7YjDq", "azSevtqA1GFz9T7YjDq", "更新疏散情况", "PassengerGroup", "乘客群", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:da4nTtpT9A3JrWQc21n", "da4nTtpT9A3JrWQc21n", "生成关联", "LineWithTrain", "线路与列车", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:fHdltixbuKnXUv5r9F6", "fHdltixbuKnXUv5r9F6", "车站无积水", "Station", "车站", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:jtIxDyIRLVxMZw7Tq35", "jtIxDyIRLVxMZw7Tq35", "执行方案", "PlanExecutor", "方案执行器", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:mhBBbVgeJS6lgSxIXKU", "mhBBbVgeJS6lgSxIXKU", "区间积水", "MetroSection", "地铁区间", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:nVjHf2KrmdIijCrca8a", "nVjHf2KrmdIijCrca8a", "更新受困情况", "PassengerGroup", "乘客群", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:nWFZ6HM9KiTV4Fj3PPz", "nWFZ6HM9KiTV4Fj3PPz", "过程1", "Scene1", "场景1", "瞬时算子", false, ""), new GromaModuleRuntime.ProcessDescriptor("ent:rxdnGDCWlbpOGBwd3RG", "rxdnGDCWlbpOGBwd3RG", "判断文件是否存在", "FileService", "文件服务", "瞬时算子", false, ""));

    public static final List<GromaModuleRuntime.TypeDescriptor> TYPES = List.of(new GromaModuleRuntime.TypeDescriptor("PassengerGroup", "乘客群", "3Dr4LeLCGkprqwEd722", List.of(new GromaMemberMeta("MlorlwiBiZh8KU0r87s", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("5x0OfMIlLsvkJCzGNcQ", "总人数", "totalPeopleCount", "BASE", "long", ""), new GromaMemberMeta("AnOLDYjpTl52BIvOCMh", "乘客状态", "passengerState", "ENUM", "com.groma.models.emergencydeduction.model.enums.PassengerStateEnum", ""), new GromaMemberMeta("eTFRIJF4ZA5HaxhN1gf", "首次受困时刻", "firstTrappedTime", "BASE", "double", ""), new GromaMemberMeta("QM6Hrha06zlLEKXTv5H", "全部安全时刻", "allSafeTime", "BASE", "double", ""), new GromaMemberMeta("15RcT4aahOAxSrDBdQi", "已安全人数", "safePeopleCount", "BASE", "long", ""), new GromaMemberMeta("IY82VYr6qLtgvSCldj7", "是否经历危险水深", "experiencedDangerDepth", "BASE", "boolean", ""))), new GromaModuleRuntime.TypeDescriptor("GeoRoute", "地理路线", "3e8a3fd5eea84ed5a3a97e97cfbf8b21", List.of(new GromaMemberMeta("d8c2a35176844ee2b88e141d1630fdc2", "坐标组", "coordinateGroup", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.GeoPointEntity>", ""))), new GromaModuleRuntime.TypeDescriptor("FileType", "文件类型", "53fc8e5ecfa54f1ba763324fcc8ff234", List.of(new GromaMemberMeta("abc7c313392342d8bb2f1a44ef593248", "路径", "path", "BASE", "java.lang.String", ""), new GromaMemberMeta("f4b4b5db6ed34dab888b16dd765d6e1d", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("cac814e0b36746918e3fc1417d61a2b9", "类型", "type", "BASE", "java.lang.String", ""), new GromaMemberMeta("bda2971e273d43348520736daa19043f", "大小", "size", "BASE", "double", ""), new GromaMemberMeta("8bf20fa960b54e8c9fd6447f3b56a3aa", "创建时间", "createTime", "BASE", "long", ""), new GromaMemberMeta("a866e42f8fda44218b66fd816ea74285", "修改时间", "modifyTime", "BASE", "long", ""), new GromaMemberMeta("60d738e97a4e475ca0fac8ded93eb673", "哈希值", "hashValue", "BASE", "java.lang.String", ""))), new GromaModuleRuntime.TypeDescriptor("GeoRegion", "地理区域", "6a9f3429d638424c85ed1b7eb01d81b8", List.of(new GromaMemberMeta("9a2ff5616c184ce68b0c3c5eb5480da5", "坐标组", "coordinateGroup", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.GeoPointEntity>", ""))), new GromaModuleRuntime.TypeDescriptor("MetroSection", "地铁区间", "6iap5H5evnoP56yto1c", List.of(new GromaMemberMeta("T3HPIYB5pFkj92hQYOb", "长度", "length", "BASE", "double", "m"), new GromaMemberMeta("OEelBaTieUbL1NIgO4p", "等效储水宽度", "equivalentStorageWidth", "BASE", "double", "m"), new GromaMemberMeta("pHyh35RSnAbxKHUj22z", "危险水深", "dangerDepth", "BASE", "double", "m"), new GromaMemberMeta("NzI5uNNldDiLgyxPW0y", "迫停水深", "forcedStopDepth", "BASE", "double", "m"), new GromaMemberMeta("xp71OUmAuU9BDQjVrDQ", "积水量", "waterVolume", "BASE", "double", "m³"), new GromaMemberMeta("SGp0FdRmQRJCz9bv1VR", "代表性水深", "representativeDepth", "BASE", "double", "m"), new GromaMemberMeta("WtxrTa8TynYayEOJ5MS", "是否放行", "releaseAllowed", "BASE", "boolean", ""), new GromaMemberMeta("B3nQ0PWoCetoCSP8gsG", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("yVYUyQOGpVapLJTqhyW", "位置", "position", "REF", "com.groma.models.emergencydeduction.model.entity.GeoRouteEntity", ""), new GromaMemberMeta("rI0LtzJ7YZcnDOyE18I", "代表底高程", "representativeFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("Iz8YFJa7WCQJipUfkV4", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum", ""))), new GromaModuleRuntime.TypeDescriptor("SectionWithTrain", "区间与区间内列车", "9ADRltdLnwn0Xr8dmcT", List.of(new GromaMemberMeta("vg1gpRPbsoxrtQkhrDO", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("N6oxK59ZIJFsieHFjtw", "区间", "section", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("Z87fcv6mx4wqsz11jWe", "已行驶距离", "travelledDistance", "BASE", "double", ""), new GromaMemberMeta("0KoAaqP8B4qvCCaYplK", "目标车站", "targetStation", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("PH2cPoljeC0p43Prsld", "出发车站", "departureStation", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("bWCZ6rcu8LXoqxCT1TG", "是否到达目标车站", "arrivedTargetStation", "BASE", "boolean", ""))), new GromaModuleRuntime.TypeDescriptor("DisposalPlan", "处置方案", "CxXzMPDWEghBGY5Xyl7", List.of(new GromaMemberMeta("51GmDUCZUE3bvJyQAPr", "方案编号", "planCode", "BASE", "java.lang.String", ""), new GromaMemberMeta("zCymV4rfDUqj9GavE6o", "方案名称", "planName", "BASE", "java.lang.String", ""), new GromaMemberMeta("NqI2ixqqjhZg3mIgc7I", "关联列车", "associatedTrain", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("nIrxHR9S6EvG66TIyVD", "列车通过所需时间", "trainPassTime", "BASE", "double", ""), new GromaMemberMeta("ze68hlsyh2tOYLCtdWk", "停运下达时刻", "suspensionIssueTime", "BASE", "double", ""), new GromaMemberMeta("ruy5VzXmYd3iZMkNX7i", "停止放行时刻", "releaseStopTime", "BASE", "double", ""))), new GromaModuleRuntime.TypeDescriptor("AreaWithStation", "区域与区域内车站", "GY5EmLZdeZGsXBJcHef", List.of(new GromaMemberMeta("gEzBWiOUbrpvxwZ8Pv0", "区域", "area", "REF", "com.groma.models.emergencydeduction.model.entity.AreaEntity", ""), new GromaMemberMeta("f482bs8kk6zJ2XuV6BK", "车站", "station", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("XLTMneA2bEyAdE4Auta", "当前过水流量", "currentFlowRate", "BASE", "double", "m³/min"), new GromaMemberMeta("vjXgOFLkILFHLg8eh5U", "本步过水量", "stepFlowVolume", "BASE", "double", "0"))), new GromaModuleRuntime.TypeDescriptor("FileService", "文件服务", "GtRoliXRxTpIg4tV7le", List.of()), new GromaModuleRuntime.TypeDescriptor("Rainfall", "降雨", "KVueSWbpHnSmDysqWWj", List.of()), new GromaModuleRuntime.TypeDescriptor("LineWithSection", "线路与区间", "Kj4rOn7Z6eIvs73yjiW", List.of(new GromaMemberMeta("gvDsKHf6YCqyP9eMVXx", "地铁线路", "metroLine", "REF", "com.groma.models.emergencydeduction.model.entity.MetroLineEntity", ""), new GromaMemberMeta("gUjJuzUZZMDkhW2LEZ5", "地铁区间", "metroSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("NxqGJkz3z98mPhNtuuM", "区间起点里程", "sectionStartMileage", "BASE", "double", ""), new GromaMemberMeta("2LrLyk9y9KZy4NbmMDp", "区间终点里程", "sectionEndMileage", "BASE", "double", ""))), new GromaModuleRuntime.TypeDescriptor("LineWithTrain", "线路与列车", "LM3IeUACJIRWE5PKqrG", List.of(new GromaMemberMeta("I74UeI1njzX6WjftjtR", "线路", "line", "REF", "com.groma.models.emergencydeduction.model.entity.MetroLineEntity", ""), new GromaMemberMeta("OxQTIIgXFvoh8WDwjOh", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("h2d1gowrlEtddEalcvT", "列车当前在线路上的里程", "trainMileageOnLine", "BASE", "double", "m"), new GromaMemberMeta("u79Sh40LbloyGRW4zMk", "沿里程增大方向", "alongIncreasingMileage", "BASE", "boolean", ""), new GromaMemberMeta("0SOv8AQOpa2y8sC7shh", "当前停靠关联", "currentParkedRelation", "REF", "com.groma.models.emergencydeduction.model.entity.StationWithParkedTrainEntity", ""), new GromaMemberMeta("x3P9T6T0WTa3KXjrMbT", "当前行驶关联", "currentRunningRelation", "REF", "com.groma.models.emergencydeduction.model.entity.SectionWithTrainEntity", ""), new GromaMemberMeta("VwizqIil29sv75BuLPm", "演示往返", "demoRoundTrip", "BASE", "boolean", ""))), new GromaModuleRuntime.TypeDescriptor("StationWithPassengerGroup", "车站与站内乘客群", "NKsxPhV6qn51BWMo1L7", List.of(new GromaMemberMeta("BcAXKtQjcKmqR1ZbpE6", "乘客群", "passengerGroup", "REF", "com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity", ""), new GromaMemberMeta("bop7naa2mk9nq5jDfQV", "车站", "station", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("KgtcWRWkJVglELd7Wcv", "当前站内人数", "currentStationPeopleCount", "BASE", "long", ""), new GromaMemberMeta("GIri3KzuDHHRylbTLId", "已疏散人数", "evacuatedPeopleCount", "BASE", "long", ""), new GromaMemberMeta("fm6lWpiMy0su6wtfeah", "疏散是否启动", "evacuationStarted", "BASE", "boolean", ""), new GromaMemberMeta("a4rKo4qnNcaqAtngpGz", "疏散状态", "evacuationState", "ENUM", "com.groma.models.emergencydeduction.model.enums.EvacuationStateEnum", ""))), new GromaModuleRuntime.TypeDescriptor("StationWithParkedTrain", "车站与停靠列车", "Pcp7gmk5wwGxDtnmiCR", List.of(new GromaMemberMeta("Y73IW1ewl3mlUJlQvWU", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("lrd9m2v82kyD3mrdk8p", "车站", "station", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("wSFHvbuLXhbgxEbboPe", "实际离站时刻", "actualDepartureTime", "BASE", "long", ""), new GromaMemberMeta("HAIVzK7lukwXbFKbuIu", "实际到站时刻", "actualArrivalTime", "BASE", "long", ""), new GromaMemberMeta("Bslj13aZSBZZ50cq6uY", "规定停站时长", "prescribedStopDuration", "BASE", "long", "s"), new GromaMemberMeta("fOJkK885q6NfNdBgF34", "离站是否允许", "departureAllowed", "BASE", "boolean", ""))), new GromaModuleRuntime.TypeDescriptor("MetroLine", "地铁线路", "TACREef9cnQRxb0lPDt", List.of(new GromaMemberMeta("Osip4AKZj87pYuthVsO", "线路名称", "lineName", "BASE", "java.lang.String", ""), new GromaMemberMeta("myTbJXCWLUt9n1c6X9A", "起点里程", "startMileage", "BASE", "double", "m"), new GromaMemberMeta("efdxuWnrUmLGrfyi3fs", "终点里程", "endMileage", "BASE", "double", "m"))), new GromaModuleRuntime.TypeDescriptor("RainfallAffectedArea", "降雨与受影响区域", "ZYR2XnvOQL0vp965s7q", List.of(new GromaMemberMeta("GLWeuTcB89MzugElwt0", "降雨", "rainfall", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallEntity", ""), new GromaMemberMeta("qtEOSTOdGnKsYsTZdtB", "区域", "area", "REF", "com.groma.models.emergencydeduction.model.entity.AreaEntity", ""), new GromaMemberMeta("fgKKe41OQVCpWalhNdk", "当前汇水流量", "currentCatchmentFlow", "BASE", "double", "m³/s"), new GromaMemberMeta("HndwguqaTkiyqJNPDMm", "本步进汇水量", "stepCatchmentVolume", "BASE", "double", "m³"), new GromaMemberMeta("hCyfzjBn9ku9OmkUZGN", "当前雨强", "currentRainIntensity", "BASE", "double", "mm/min"), new GromaMemberMeta("xBY1ctaZuFkqQteAfD7", "累计雨量", "cumulativeRainfall", "BASE", "double", "mm"), new GromaMemberMeta("DN3St5fHDF0AIdYtDNY", "雨量序列", "rainfallSeries", "MAP", "java.util.Map<java.lang.Long, java.lang.Double>", ""))), new GromaModuleRuntime.TypeDescriptor("PlanExecutor", "方案执行器", "bSGKABguV3sqda7gC7w", List.of(new GromaMemberMeta("DtQr9wp5Yd34LeqZVp6", "在行方案", "activePlan", "REF", "com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity", ""), new GromaMemberMeta("OJJ7TO7kt5NUf79Sizn", "当前方案阶段", "currentPlanStage", "ENUM", "com.groma.models.emergencydeduction.model.enums.TaskStateEnum", ""), new GromaMemberMeta("WqYtoJd7SckcWKXUotn", "实际下达时刻", "actualIssueTime", "BASE", "double", ""), new GromaMemberMeta("uJqrJjseEN6Su0n5lQ5", "实际生效时刻", "actualEffectiveTime", "BASE", "double", ""))), new GromaModuleRuntime.TypeDescriptor("LineWithStation", "线路与车站", "bU09sM7lboHNQ4yfpX8", List.of(new GromaMemberMeta("RMXGiHtWwfLlhtvc2qN", "地铁线路", "metroLine", "REF", "com.groma.models.emergencydeduction.model.entity.MetroLineEntity", ""), new GromaMemberMeta("WaXfPfoMap0FvB1b0Sf", "车站", "station", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("qAxmfURJraF6VumzkyH", "线路里程", "lineMileage", "BASE", "double", "m"), new GromaMemberMeta("X6iOVMMRFEgOu7oquve", "站点类型", "stationType", "ENUM", "com.groma.models.emergencydeduction.model.enums.StationTypeEnum", ""), new GromaMemberMeta("FGwVziXbhicx6BqbRNe", "站号", "stationNo", "BASE", "long", ""))), new GromaModuleRuntime.TypeDescriptor("GeoPoint", "地理点位", "c4998ee1638a4a9ca0029f21d57850d4", List.of(new GromaMemberMeta("c66c8d5f512d410c8667aea645329f8a", "经度", "longitude", "BASE", "double", ""), new GromaMemberMeta("c345363c20924664b8e94fe5188df806", "纬度", "latitude", "BASE", "double", ""), new GromaMemberMeta("138cb6dd6b224361b9fe2a08f0beca74", "高度", "height", "BASE", "double", ""))), new GromaModuleRuntime.TypeDescriptor("Area", "区域", "eIdrNAXcjZ1OqUVr3LB", List.of(new GromaMemberMeta("akg8YG3yPjlwkHg2h7v", "汇水面积", "catchmentArea", "BASE", "double", "m²"), new GromaMemberMeta("0cMt7C0dg5JuQEXCcbw", "径流系数", "runoffCoeff", "BASE", "double", "0.8"), new GromaMemberMeta("98fvn5ZHm7y1PFWygyd", "等效蓄水面积", "equivalentStorageArea", "BASE", "double", "m²"), new GromaMemberMeta("w3KEShYZsq5e07tNgCu", "地面高程", "groundElevation", "BASE", "double", "m"), new GromaMemberMeta("ISBiulbqRYu9DTLTdBX", "地表排水能力", "surfaceDrainCapacity", "BASE", "double", "m³/min"), new GromaMemberMeta("I5aMxoukyy1DY1moXdW", "积水量", "waterVolume", "BASE", "double", "m³"), new GromaMemberMeta("dcrXMnbxACceh8RH8aG", "积水深度", "waterDepth", "BASE", "double", "m"), new GromaMemberMeta("z2JtEUZfeWAxkF9vaof", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("nDmWtXMO1R0KFEhdC1B", "位置", "position", "REF", "com.groma.models.emergencydeduction.model.entity.GeoRegionEntity", ""), new GromaMemberMeta("pdiEfPgAApLsNQ1Cp5j", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum", ""))), new GromaModuleRuntime.TypeDescriptor("FileInfo", "文件信息", "kBqMrvjfh8tkTrIKWEk", List.of(new GromaMemberMeta("Qw90Ju01RwLt1yd5L4G", "创建时间", "createTime", "BASE", "long", ""), new GromaMemberMeta("ULUgi4WDxQKuBMJXuen", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("KAdhc4M6XrYJ6Qwf5Gr", "哈希值", "hashValue", "BASE", "java.lang.String", ""), new GromaMemberMeta("fW746fHkt66FWhN4YM1", "大小", "size", "BASE", "double", ""), new GromaMemberMeta("JxS0RFjhdtbYQPcgLkA", "类型", "type", "BASE", "java.lang.String", ""), new GromaMemberMeta("qeT9LBbjlW9XrzCmI6S", "修改时间", "modifyTime", "BASE", "long", ""))), new GromaModuleRuntime.TypeDescriptor("TrainWithPassengerGroup", "列车与车内乘客群", "nYxypzPOPAMpit1FFqW", List.of(new GromaMemberMeta("ZcaVkzem6xwLhB1el0Q", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""), new GromaMemberMeta("0U8JtY9HR2pGe4LO3TV", "乘客群", "passengerGroup", "REF", "com.groma.models.emergencydeduction.model.entity.PassengerGroupEntity", ""), new GromaMemberMeta("1EXKYn7RxkMlFg71Dif", "当前在车人数", "currentOnBoardCount", "BASE", "long", ""))), new GromaModuleRuntime.TypeDescriptor("PlanGenerator", "方案生成器", "nd5eUBHKb67qBOkAHs9", List.of(new GromaMemberMeta("lyn4vqIguoVbutUdARW", "备选方案列表", "candidatePlanList", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity>", ""), new GromaMemberMeta("7sd1gdSUNacuOMI7apK", "降雨", "rainfall", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallEntity", ""), new GromaMemberMeta("Y11bqLOOJYHpIDUyHQv", "地面积水区", "surfaceWaterloggingArea", "REF", "com.groma.models.emergencydeduction.model.entity.AreaEntity", ""), new GromaMemberMeta("qUcAbt0pZHr081flS5W", "挡水墙", "floodWall", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("f5AiOO4UlGpWYjtGOOS", "地铁区间", "metroSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("LrofSrmMKwJjCcMPQiW", "列车", "train", "REF", "com.groma.models.emergencydeduction.model.entity.TrainEntity", ""))), new GromaModuleRuntime.TypeDescriptor("Station", "车站", "rFXJKdFfEChoRdeX7aT", List.of(new GromaMemberMeta("ZSCBmOks5hfDvQgGwnd", "站内积水量", "stationWaterVolume", "BASE", "double", "m³"), new GromaMemberMeta("9cgmJtgAfA5THCTQX1Q", "失效超高阈值", "failureThreshold", "BASE", "double", "m"), new GromaMemberMeta("2x8UFMHABjbk7XdvGSq", "失效持续时间", "failureDuration", "BASE", "double", "s"), new GromaMemberMeta("HsJlhF4Qq0meQjVXQl5", "挡水墙顶高程", "floodWallTopElevation", "BASE", "double", "m"), new GromaMemberMeta("icz3z4NdDl4XzMF1lEA", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("ySKDJJ9ZM0cRG8ny4A4", "排水额定能力", "ratedDrainCapacity", "BASE", "double", "m³/min"), new GromaMemberMeta("JbCoZ3N3pGfxVn1fAHA", "站内底高程", "stationFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("TGW59nqUVYBoQzfbnSa", "等效蓄水面积", "equivalentStorageArea", "BASE", "double", "m²"), new GromaMemberMeta("4BkvxU1jidI2jo6QEpq", "站内水深", "stationWaterDepth", "BASE", "double", "m"), new GromaMemberMeta("xLpWusnJR2Aw0xM8gei", "疏散能力", "evacuationCapacity", "BASE", "double", "人/min"), new GromaMemberMeta("bsd5Noel9k5tlgpoOWP", "位置", "position", "REF", "com.groma.models.emergencydeduction.model.entity.GeoPointEntity", ""), new GromaMemberMeta("ZYx31ZY8GstGNur40z2", "积水状态", "waterloggingState", "ENUM", "com.groma.models.emergencydeduction.model.enums.WaterloggingStateEnum", ""), new GromaMemberMeta("KsKWUZghMV0qp4PvIP9", "挡水墙状态", "floodWallState", "ENUM", "com.groma.models.emergencydeduction.model.enums.FloodWallStateEnum", ""), new GromaMemberMeta("4y48YnVOoSCjQWsqZcs", "完好时过水宽度", "intactFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("TmBu06RLACpGgpetjrU", "失效后过水宽度", "failedFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("lcRigegnMIZiDTVJben", "入口底高程", "inletFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("CbhmI1QG31xuE1fsd8M", "入口流量系数", "inletFlowCoeff", "BASE", "double", ""), new GromaMemberMeta("SnxMu3A2siAAnOyycRr", "超限累计时间", "overlimitDuration", "BASE", "double", "s"))), new GromaModuleRuntime.TypeDescriptor("Train", "列车", "rWqQrIoiDfDYWa5PiWk", List.of(new GromaMemberMeta("NAfVPmumtjToqjUB8dS", "当前速度", "currentSpeed", "BASE", "double", "m/min"), new GromaMemberMeta("mKbdhAWlYJQzae7Zs2Y", "迫停时刻", "forcedStopTime", "BASE", "double", "s"), new GromaMemberMeta("6q4BLT4xe5rzYrXrlde", "初始位置", "initialPosition", "REF", "com.groma.models.emergencydeduction.model.entity.GeoPointEntity", ""), new GromaMemberMeta("a0gEhiRbo6Z0gnO5dir", "名称", "name", "BASE", "java.lang.String", ""), new GromaMemberMeta("mQs84jpKcMkngxFWdBT", "列车状态", "trainState", "ENUM", "com.groma.models.emergencydeduction.model.enums.TrainStateEnum", ""), new GromaMemberMeta("VnDxdvqY8EYBbJN0JlM", "方向", "direction", "BASE", "double", ""), new GromaMemberMeta("f2Z0TqRbENyGCigHGVL", "当前位置", "currentPosition", "REF", "com.groma.models.emergencydeduction.model.entity.GeoPointEntity", ""), new GromaMemberMeta("cgZTF4S44iiPjp29iem", "行驶速度", "travelSpeed", "BASE", "double", "m/min"))), new GromaModuleRuntime.TypeDescriptor("SectionWithEndStation", "区间与端点车站", "yALuBQFoqC0OJuH31TP", List.of(new GromaMemberMeta("cs7DzmGZqyZnfP20Gsy", "车站", "station", "REF", "com.groma.models.emergencydeduction.model.entity.StationEntity", ""), new GromaMemberMeta("6NOAfgYaMv32vdr5VlF", "区间", "section", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""), new GromaMemberMeta("KMlHnXHXmSsrivSkbmf", "本步过水量", "stepFlowVolume", "BASE", "double", "m³"), new GromaMemberMeta("gfXSjsjhf0iZULvQE6F", "连接处底高程", "junctionFloorElevation", "BASE", "double", "m"), new GromaMemberMeta("aqBhFinNqNUJOw2rYuA", "有效过水宽度", "effectiveFlowWidth", "BASE", "double", "m"), new GromaMemberMeta("oSBmVrsD3jJmoAhvGHi", "水流系数", "flowCoeff", "BASE", "double", ""), new GromaMemberMeta("QLFX5LYS3Oabdm7w2ZM", "当前过水流量", "currentFlowRate", "BASE", "double", "m³/min"))), new GromaModuleRuntime.TypeDescriptor("Scene2", "场景2", "fFnAAr3WKsN0Oaghvzj", List.of(new GromaMemberMeta("OlRd4AmwiiBoH9RgrTR", "降雨关联区域", "rainfallRelatedArea", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallAffectedAreaEntity", ""), new GromaMemberMeta("LBJJ0xxTgEtmyb9GiZu", "区域与区域内车站", "areaWithStation", "REF", "com.groma.models.emergencydeduction.model.entity.AreaWithStationEntity", ""), new GromaMemberMeta("HgVFnRjnjDVAUqiEA2V", "区间与端点车站", "sectionWithEndStation", "REF", "com.groma.models.emergencydeduction.model.entity.SectionWithEndStationEntity", ""), new GromaMemberMeta("xTCY2kcgoHtoR9oL0Ka", "线路与列车", "lineWithTrain", "REF", "com.groma.models.emergencydeduction.model.entity.LineWithTrainEntity", ""))), new GromaModuleRuntime.TypeDescriptor("Scene1", "场景1", "bX4vMzSlVMdk6QjF7CD", List.of(new GromaMemberMeta("xuFANgtC3Dzw6jUE1H4", "列车状态1", "trainState1", "ENUM", "com.groma.models.emergencydeduction.model.enums.TrainStateEnum", ""), new GromaMemberMeta("GKzjQYuz4zLjBQGdWxy", "文本1", "text1", "BASE", "boolean", ""), new GromaMemberMeta("Cwaojup8iZOpBuisiCL", "文本2", "text2", "BASE", "java.lang.String", ""))), new GromaModuleRuntime.TypeDescriptor("TrainPassing", "列车通行", "8iJr06uEbFRp2d7gT5y", List.of(new GromaMemberMeta("b1GaRdiCLb2mOttmees", "通行列车", "passingTrain", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.TrainEntity>", ""), new GromaMemberMeta("Mw6y78qyiaIYPO6RwDY", "通行区间", "passingSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""))), new GromaModuleRuntime.TypeDescriptor("UndergroundInflow", "地下进水", "BdhINdjBhLdgkp89TAf", List.of(new GromaMemberMeta("9xOpimmMwHIjrS4tBB4", "地面积水来源", "surfaceWaterloggingSource", "REF", "com.groma.models.emergencydeduction.model.entity.AreaEntity", ""), new GromaMemberMeta("aeMBq8a9ECoL0Cubhuc", "进水区间", "inflowSection", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.MetroSectionEntity>", ""))), new GromaModuleRuntime.TypeDescriptor("RainfallCatchment", "降雨汇水", "E32hWqA7cw2F3sNSc8K", List.of(new GromaMemberMeta("ba15DPUPM07QgiJWWfb", "降雨来源", "rainfallSource", "REF", "com.groma.models.emergencydeduction.model.entity.RainfallEntity", ""), new GromaMemberMeta("Shu45x1YIGfSadlCre9", "汇水区域", "catchmentArea", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.AreaEntity>", ""))), new GromaModuleRuntime.TypeDescriptor("FloodProtection", "挡水防护", "a0FNVLNrTFTjGHd2bYh", List.of(new GromaMemberMeta("m0CoVE4j8kj040bE79P", "防护挡水墙", "protectiveFloodWall", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.StationEntity>", ""), new GromaMemberMeta("bwGKCA8ZWLc3ok0uzQz", "防护区间", "protectedSection", "REF", "com.groma.models.emergencydeduction.model.entity.MetroSectionEntity", ""))), new GromaModuleRuntime.TypeDescriptor("SuspensionControl", "停运控制", "kFr6nPxVVgIlStfwOCS", List.of(new GromaMemberMeta("QCg39qqOAAmSMQKtnOq", "决策方案", "decisionPlan", "REF", "com.groma.models.emergencydeduction.model.entity.DisposalPlanEntity", ""), new GromaMemberMeta("yxNQg49WcLIlXYaYnu4", "控制区间", "controlSection", "LIST", "java.util.List<com.groma.models.emergencydeduction.model.entity.MetroSectionEntity>", ""))));

    private ProcessCatalog() {
    }

    /**
     * 按过程定义键新建过程任务（未注册的键返回 null）
     */
    public static GromaProcessTask newTask(String defKey, GromaWorldContext ctx, BaseSimEntity host,
            Map<String, Object> args) {
        switch (defKey) {
            case "dv:designView_04fb48efd78444e3bfc948e0fa91c1f3" -> {
                return new FileTypeFileExistsProcessTask(ctx, (FileTypeEntity) host, b(args, "是否存在"));
            }
            case "dv:designView_0joI684F5zzw3QGvIDY" -> {
                return new StationInletFlowProcessTask(ctx, (StationEntity) host, b(args, "向站内流入"), d(args, "区域水面高程"), l(args, "本步秒数"), d(args, "当前过水流量"), d(args, "站内水面高程"), d(args, "本步过水量"), d(args, "区域积水量"));
            }
            case "dv:designView_175Y0QSMw786DGgXBXZ" -> {
                return new AreaWaterloggingChangeProcessTask(ctx, (AreaEntity) host, d(args, "水量变化值"));
            }
            case "dv:designView_1BqELDRmZFoiytzNKQ5" -> {
                return new AreaSurfaceDrainProcessTask(ctx, (AreaEntity) host);
            }
            case "dv:designView_1tvmsrr8ydvpCLGAoxr" -> {
                return new TrainAdvanceAlongSectionProcessTask(ctx, (TrainEntity) host, (MetroSectionEntity) (args == null || args.get("地铁区间") == null ? null : GromaValues.decode(args.get("地铁区间"), ctx)), (StationEntity) (args == null || args.get("目标车站") == null ? null : GromaValues.decode(args.get("目标车站"), ctx)), d(args, "行驶间隔"), d(args, "已行驶距离"), b(args, "是否沿里程增大方向"), d(args, "当前线路里程"));
            }
            case "dv:designView_218a29d6cd004955b234ad6b1bafc7b6" -> {
                return new FileTypeIsEmptyFileProcessTask(ctx, (FileTypeEntity) host, b(args, "是否为空"));
            }
            case "dv:designView_2WLvrJyBv6htmVscnRk" -> {
                return new Scene12TurnNoIntoYesProcessTask(ctx, (Scene1Entity) host, b(args, "文本1"));
            }
            case "dv:designView_2aI60V2gYdhoWiHC5B3" -> {
                return new StationFlowExchangeIntervalProcessTask(ctx, (StationEntity) host, l(args, "本步时长"));
            }
            case "dv:designView_3xY5i4HkUNciNe3ll79" -> {
                return new RainfallGetRainfallSeriesProcessTask(ctx, (RainfallEntity) host, (Map<Long, Double>) (args == null || args.get("雨量序列新") == null ? null : GromaValues.decode(args.get("雨量序列新"), ctx)), (Map<Long, Double>) (args == null || args.get("原雨量序列") == null ? null : GromaValues.decode(args.get("原雨量序列"), ctx)));
            }
            case "dv:designView_5mX6EkRtocaf9rXbKu1" -> {
                return new MetroSectionStationSectionFlowExchangeProcessTask(ctx, (MetroSectionEntity) host, d(args, "水流系数"), d(args, "连接处底高程"), d(args, "本步过水量"), (StationEntity) (args == null || args.get("端点车站") == null ? null : GromaValues.decode(args.get("端点车站"), ctx)), d(args, "当前过水流量"), d(args, "有效过水宽度"));
            }
            case "dv:designView_659efee68fd74a0a86fa968e044a59aa" -> {
                return new FileTypeFileInitProcessTask(ctx, (FileTypeEntity) host);
            }
            case "dv:designView_65ClRS3n9vUb3935COH" -> {
                return new FindAheadStationProcessTask(ctx, (LineWithTrainEntity) (args == null || args.get("当前线路与列车") == null ? null : GromaValues.decode(args.get("当前线路与列车"), ctx)), b(args, "是否找到"), (LineWithStationEntity) (args == null || args.get("前方线路与车站") == null ? null : GromaValues.decode(args.get("前方线路与车站"), ctx)));
            }
            case "dv:designView_767z0mfmOaNst2dPwlG" -> {
                return new TrainDepartProcessTask(ctx, (TrainEntity) host, (StationWithParkedTrainEntity) (args == null || args.get("车站与停靠列车") == null ? null : GromaValues.decode(args.get("车站与停靠列车"), ctx)));
            }
            case "dv:designView_80233ba35326427aa7886a6ec6051604" -> {
                return new FileTypeReadTextContentProcessTask(ctx, (FileTypeEntity) host, s(args, "文件内容"));
            }
            case "dv:designView_BPlSUsxYsStEoFQEAGD" -> {
                return new DetermineBothWaterLevelsProcessTask(ctx, d(args, "第一侧底高程"), d(args, "第一侧水面高程"), d(args, "第一侧水深"), d(args, "第二侧水面高程"), d(args, "第二侧底高程"), d(args, "第二侧水深"));
            }
            case "dv:designView_DwYZrRL0fsJBQ7H79np" -> {
                return new MetroSectionWaterDepthChangeProcessTask(ctx, (MetroSectionEntity) host);
            }
            case "dv:designView_FVDIGUCMq2RHeXb9Wgr" -> {
                return new MetroSectionFlowExchangeIntervalProcessTask(ctx, (MetroSectionEntity) host, l(args, "本步秒数"));
            }
            case "dv:designView_H4eBTelUToW12IqLJ8O" -> {
                return new DetermineJunctionFlowProcessTask(ctx, d(args, "过水控制高程"), d(args, "过水宽度"), d(args, "第一侧水面高程"), d(args, "当前过水流量"), d(args, "第一侧积水量"), b(args, "第一侧流向第二侧"), d(args, "第二侧积水量"), d(args, "本步过水量"), d(args, "第二侧水面高程"), l(args, "本步秒数"), d(args, "流量系数"));
            }
            case "dv:designView_JTcC0L3UTKJxpFJ1ods" -> {
                return new GetStepDurationProcessTask(ctx, l(args, "本步秒数"));
            }
            case "dv:designView_P80bc3nl6S9bWISn6bg" -> {
                return new PlanGeneratorComputeDisposalTimeProcessTask(ctx, (PlanGeneratorEntity) host);
            }
            case "dv:designView_XLrKPtiJlmUbySIChuF" -> {
                return new TrainSectionRunProcessTask(ctx, (TrainEntity) host, (SectionWithTrainEntity) (args == null || args.get("区间与区间内列车") == null ? null : GromaValues.decode(args.get("区间与区间内列车"), ctx)), d(args, "当前线路里程"), b(args, "是否沿里程增大方向"));
            }
            case "dv:designView_aroqnVQzhKVnM1h7wPt" -> {
                return new AreaComputeRainfallCatchmentProcessTask(ctx, (AreaEntity) host, d(args, "当前雨强"), d(args, "当前汇水流量"), d(args, "本步汇水量"));
            }
            case "dv:designView_bXHqX2hJtSvKqqleN8E" -> {
                return new MetroSectionSectionFlowBackStationProcessTask(ctx, (MetroSectionEntity) host, (StationEntity) (args == null || args.get("车站") == null ? null : GromaValues.decode(args.get("车站"), ctx)), d(args, "本步过水量"));
            }
            case "dv:designView_eHq1YpIkoLxwHKywWis" -> {
                return new TrainStoppedAtStationProcessTask(ctx, (TrainEntity) host, (StationWithParkedTrainEntity) (args == null || args.get("车站与停靠列车") == null ? null : GromaValues.decode(args.get("车站与停靠列车"), ctx)));
            }
            case "dv:designView_fUFQ4qvShQtFqOghp0w" -> {
                return new StationStationFlowBackSurfaceProcessTask(ctx, (StationEntity) host, (AreaEntity) (args == null || args.get("区域") == null ? null : GromaValues.decode(args.get("区域"), ctx)), d(args, "本步过水量"));
            }
            case "dv:designView_fjlFgY29HmzqjbmHxYC" -> {
                return new RainfallRainfallProcessTask(ctx, (RainfallEntity) host, (Map<Long, Double>) (args == null || args.get("雨量序列") == null ? null : GromaValues.decode(args.get("雨量序列"), ctx)), d(args, "当前雨强"), d(args, "累计雨量"));
            }
            case "dv:designView_i8XnCQtmJLFiB3KzdpL" -> {
                return new StationWaterloggingChangeProcessTask(ctx, (StationEntity) host, d(args, "水量变化值"));
            }
            case "dv:designView_joMlLooi2InkPOjk59H" -> {
                return new TrainEnterStationProcessTask(ctx, (TrainEntity) host);
            }
            case "dv:designView_kVXT3R1XGHTwkie2rur" -> {
                return new MetroSectionWaterloggingChangeProcessTask(ctx, (MetroSectionEntity) host, d(args, "水量变化"));
            }
            case "dv:designView_kclxVkgMIrkBe36K656" -> {
                return new MetroSectionDrainProcessTask(ctx, (MetroSectionEntity) host);
            }
            case "dv:designView_l5HF6zfm23Aq3LAjrs5" -> {
                return new MetroSectionStationFlowIntoSectionProcessTask(ctx, (MetroSectionEntity) host, (StationEntity) (args == null || args.get("车站") == null ? null : GromaValues.decode(args.get("车站"), ctx)), d(args, "本步过水量"));
            }
            case "dv:designView_miDwXHXHR407mRDMPzO" -> {
                return new Scene14CreateObjectTurnYesIntoNoProcessTask(ctx, (Scene1Entity) host, b(args, "文本1"));
            }
            case "dv:designView_rXSwECDiXYGPqX12Bir" -> {
                return new StationFloodWallBearPressureProcessTask(ctx, (StationEntity) host, l(args, "本步秒数"), d(args, "区域水面高程"));
            }
            case "dv:designView_sJx0EiISfyfjakZXrm0" -> {
                return new Scene13ConvertToRunProcessTask(ctx, (Scene1Entity) host);
            }
            case "dv:designView_tOSCSMGoda2sHYuuDqN" -> {
                return new LineWithTrainFormStationTrainRelationProcessTask(ctx, (LineWithTrainEntity) host, (StationWithParkedTrainEntity) (args == null || args.get("车站与停靠列车") == null ? null : GromaValues.decode(args.get("车站与停靠列车"), ctx)));
            }
            case "dv:designView_vFHwnod6GqHo3Bde4P5" -> {
                return new Scene11ConvertToStopProcessTask(ctx, (Scene1Entity) host);
            }
            case "dv:designView_xmjSEU4SGv2otlq6mLP" -> {
                return new StationAreaStationFlowExchangeProcessTask(ctx, (StationEntity) host, d(args, "当前过水流量"), d(args, "本步过水量"), (AreaEntity) (args == null || args.get("区域") == null ? null : GromaValues.decode(args.get("区域"), ctx)));
            }
            case "dv:designView_yYvJXg9UMsYaNFITgoc" -> {
                return new StationSurfaceFlowIntoStationProcessTask(ctx, (StationEntity) host, (AreaEntity) (args == null || args.get("区域") == null ? null : GromaValues.decode(args.get("区域"), ctx)), d(args, "本步过水量"));
            }
            case "dv:designView_z02AJALkr13QaoAQd4l" -> {
                return new LineWithTrainFormSectionTrainRelationProcessTask(ctx, (LineWithTrainEntity) host, (SectionWithTrainEntity) (args == null || args.get("区间与区间内列车") == null ? null : GromaValues.decode(args.get("区间与区间内列车"), ctx)));
            }
            case "ent:3UULlXsUgqgKXOiinlG" -> {
                return new FileServiceReadTextContentProcessTask(ctx, (FileServiceEntity) host, s(args, "路径/标识"), b(args, "是否成功"), s(args, "文件内容"));
            }
            case "ent:3uWavwYkJfsqvNp2BuZ" -> {
                return new RainfallProcedure1ProcessTask(ctx, (RainfallEntity) host, (Map<Long, Double>) (args == null || args.get("文本1") == null ? null : GromaValues.decode(args.get("文本1"), ctx)));
            }
            case "ent:9W9vzQHklaUg4QG70Vc" -> {
                return new AreaToWaterloggedStateProcessTask(ctx, (AreaEntity) host);
            }
            case "ent:9gvX0IJx27OFUEQmGbX" -> {
                return new LineWithTrainCreateRelationProcessTask(ctx, (LineWithTrainEntity) host);
            }
            case "ent:AqYEeNHBfq4AjItqD8C" -> {
                return new MetroSectionSectionNoWaterloggingProcessTask(ctx, (MetroSectionEntity) host);
            }
            case "ent:FylD7oTkdptLzbz6ZeT" -> {
                return new StationFloodWallFailureProcessTask(ctx, (StationEntity) host);
            }
            case "ent:IH42eYNsj0haPC4ctyQ" -> {
                return new StationStationHasWaterloggingProcessTask(ctx, (StationEntity) host);
            }
            case "ent:IeK32FL7HMYxvSd4PnP" -> {
                return new FileServiceIsFileEmptyProcessTask(ctx, (FileServiceEntity) host, s(args, "路径/标识"), b(args, "是否成功"), b(args, "是否为空"));
            }
            case "ent:NdLStqTVimDD7Npig19" -> {
                return new AreaToNoWaterloggingStateProcessTask(ctx, (AreaEntity) host);
            }
            case "ent:Ne04mUfbnKpBEjTlARK" -> {
                return new FileServiceFileInitProcessTask(ctx, (FileServiceEntity) host, s(args, "路径/标识"), b(args, "是否成功"), (FileInfoEntity) (args == null || args.get("文件信息") == null ? null : GromaValues.decode(args.get("文件信息"), ctx)), s(args, "错误信息"));
            }
            case "ent:QQMUU7DuY51tlC5lIlP" -> {
                return new Scene1Procedure1ProcessTask(ctx, (Scene1Entity) host);
            }
            case "ent:R4yssk5cauVoZXGtqkM" -> {
                return new FileServiceReadFileLineByLineProcessTask(ctx, (FileServiceEntity) host, s(args, "路径/标识"), l(args, "批次号"), s(args, "文件内容"));
            }
            case "ent:UkKv7XkK4ASpaf1Bnit" -> {
                return new PlanGeneratorFormCandidateResponsePlanProcessTask(ctx, (PlanGeneratorEntity) host);
            }
            case "ent:XUr2ph0kb7en26PZ8gp" -> {
                return new PassengerGroupRecordDangerExposureProcessTask(ctx, (PassengerGroupEntity) host);
            }
            case "ent:azSevtqA1GFz9T7YjDq" -> {
                return new PassengerGroupUpdateEvacuationProcessTask(ctx, (PassengerGroupEntity) host);
            }
            case "ent:da4nTtpT9A3JrWQc21n" -> {
                return new LineWithTrainGenerateRelationProcessTask(ctx, (LineWithTrainEntity) host);
            }
            case "ent:fHdltixbuKnXUv5r9F6" -> {
                return new StationStationNoWaterloggingProcessTask(ctx, (StationEntity) host);
            }
            case "ent:jtIxDyIRLVxMZw7Tq35" -> {
                return new PlanExecutorExecutePlanProcessTask(ctx, (PlanExecutorEntity) host);
            }
            case "ent:mhBBbVgeJS6lgSxIXKU" -> {
                return new MetroSectionSectionWaterloggingProcessTask(ctx, (MetroSectionEntity) host);
            }
            case "ent:nVjHf2KrmdIijCrca8a" -> {
                return new PassengerGroupUpdateTrappedProcessTask(ctx, (PassengerGroupEntity) host);
            }
            case "ent:nWFZ6HM9KiTV4Fj3PPz" -> {
                return new Scene1Procedure1ProcessTask2(ctx, (Scene1Entity) host);
            }
            case "ent:rxdnGDCWlbpOGBwd3RG" -> {
                return new FileServiceFileExistsProcessTask(ctx, (FileServiceEntity) host, s(args, "路径/标识"), b(args, "是否存在"), b(args, "是否成功"));
            }
            default -> { return null; }
        }
    }

    private static double d(Map<String, Object> args, String zh) {
        return args == null || args.get(zh) == null ? 0.0D : GromaValues.toDouble(args.get(zh));
    }

    private static long l(Map<String, Object> args, String zh) {
        return args == null || args.get(zh) == null ? 0L : GromaValues.toLong(args.get(zh));
    }

    private static boolean b(Map<String, Object> args, String zh) {
        return args != null && args.get(zh) != null && GromaValues.toBoolean(args.get(zh));
    }

    private static String s(Map<String, Object> args, String zh) {
        return args == null ? null : GromaValues.toText(args.get(zh));
    }
}

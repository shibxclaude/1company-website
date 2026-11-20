// 站点级静态信息：公司、备案、导航、首页内容、FAQ、发展历程。
// 解决方案、价格、新闻由后台维护、接口提供（见 stores/catalog.js），这里只保留不需要后台维护的内容。

export const SITE_URL = 'https://www.yilinga.com'

export const company = {
  name: '北京一零科技有限公司',
  shortName: '一零科技',
  address: '北京市·海淀区上地信息路1号2号楼2104',
  phone: '13699285133',
  phoneDisplay: '136 9928 5133',
  foundingDate: '2015-05-25',
  description:
    '主要从事物联网技术应用、平台架构设计、大数据分析和云计算服务研发工作。为客户提供专业化的软件产品、系统化的解决方案和数字化的科技服务。',
  icp: { text: '京ICP备 15030540号-16', url: 'https://beian.miit.gov.cn/' },
  police: {
    text: '京公网安备11010802045296号',
    url: 'https://beian.mps.gov.cn/#/query/webSearch?code=11010802045296',
  },
}

// 导航。children 为下拉项；dynamic: 'solutions' 表示下拉项由接口数据生成。
export const nav = [
  { key: 'home', label: '首页', to: '/', mobileIcon: 'home' },
  {
    key: 'about',
    label: '关于我们',
    to: '/about',
    mobileIcon: 'about',
    children: [
      { label: '公司简介', to: '/about' },
      { label: '发展历程', to: '/about/history' },
    ],
  },
  {
    key: 'solutions',
    label: '解决方案',
    to: '/solutions',
    mobileIcon: 'solution',
    dynamic: 'solutions',
    children: [{ label: '全部解决方案', to: '/solutions' }],
  },
  {
    key: 'products',
    label: '产品中心',
    to: '/products',
    mobileIcon: 'product',
    children: [
      { label: '信息技术应用能力', to: '/products' },
      { label: '产品生态图总览', to: '/products/ecosystem' },
      { label: '价格公示', to: '/price' },
    ],
  },
  { key: 'news', label: '新闻动态', to: '/news', mobileIcon: 'news' },
  { key: 'faq', label: '常见问题', to: '/faq', mobileIcon: 'faq' },
]

export const serviceAreas = [
  { name: '政务服务', icon: '/images/zhengwu.png' },
  { name: '智慧城市物联网', icon: '/images/zhihui.png' },
  { name: '社会资源服务', icon: '/images/shehui.png' },
  { name: '营商环境平台建设', icon: '/images/yingshang.png' },
  { name: '警务信息采集', icon: '/images/jingwu.png' },
  { name: '警务数据中心建设', icon: '/images/jingwushuju.png' },
  { name: '数字化警务室建设', icon: '/images/shuzi.png' },
  { name: '多维数据融合应用平台', icon: '/images/duowei.png' },
]

export const banners = ['/images/banner01.png', '/images/banner02.png', '/images/banner03.png']

export const credentials = [
  { name: '高新技术企业', icon: 'building' },
  { name: '商标证书', icon: 'certificate' },
  { name: '软件著作权', icon: 'stack' },
  { name: '技术专利', icon: 'medal' },
]

export const aboutParagraphs = [
  '北京一零科技有限公司总部位于北京市海淀区上地信息路1号2号楼2104室，成立于2015年，研发团队以博士、硕士为主，以中科院自动化研究所、中科院遥感与数字地球研究所、清华大学、中国人民公安大学等国内著名科研院所多年技术积累和业务沉淀为基础，主要从事物联网技术应用、平台架构设计、大数据分析和云计算服务研发工作。',
  '为客户提供专业化的软件产品、系统化的解决方案和数字化的科技服务。',
]

export const history = [
  { year: '2015', events: ['北京一零科技有限公司成立，警务物联网等八个应用先后获得软件著作权'] },
  { year: '2016', events: ['公司研发应用通过公安部第一研究所、中国软件评测中心测试并获取软件产品登记报告'] },
  { year: '2017', events: ['公司5项核心技术通过国家知识产权局审核并授予专利权', '同年荣获国家高新企业证书'] },
  { year: '2018', events: ['公司联合河南省公安厅起草编制《非机动车电子标识通用技术规范》通过立项评审'] },
  { year: '2019', events: ['县域多维数据融合平台系统上线'] },
  { year: '2020', events: ['长影环球100奇幻乐园建立合作伙伴关系', '中国联通合作建设“智慧磐石”'] },
  { year: '2021', events: ['县域疫情防控系统上线，助力宁陵疫情防控', '南阳市公安局“一站一室一中心”启动'] },
  { year: '2022', events: ['市级（商丘）疫情防控码系统上线，并申报河南省大数据优秀应用案例'] },
  { year: '2023', events: ['县域（宁陵、虞城）营商环境门户系统建设'] },
  { year: '2024', events: ['与东信（北京）资产管理公司合作创立东信数智（北京）科技有限公司'] },
  { year: '2025', events: ['营商一体化领导驾驶舱系统上线', '商丘保安从业单位考试系统项目建设'] },
  { year: '2026', events: ['市级（许昌）重点目标管理系统建设', '高中阶段学校招生录取平台上线'] },
]

export const faqs = [
  {
    q: '一零科技主要服务哪些领域？',
    a: '我们专注于政务数字化、警务信息化、智慧城市物联网、数据融合平台建设四大领域，为政府及公安机关提供从数据采集、融合到智能应用的全栈解决方案。',
  },
  {
    q: '公安派出所多维数据融合平台能解决什么问题？',
    a: '整合派出所各类业务数据，打破信息孤岛，实现数据共享和智能研判，提升基层警务效能，帮助派出所实现“数据驱动决策”。',
  },
  {
    q: '智慧城市物联网平台如何部署？',
    a: '提供从设备接入、数据采集到应用开发的全链路平台，支持私有云和混合云部署，可根据客户需求灵活定制，保障数据安全。',
  },
  {
    q: '政务大数据二次开发需要多久？',
    a: '根据需求复杂度，一般2-4周可完成定制开发。我们提供标准化API接口，确保与现有系统无缝对接。',
  },
  {
    q: '一零科技在警务信息化领域有成功案例吗？',
    a: '已为多个省市公安机关提供数据融合平台和警务数字化解决方案，包括派出所多维数据融合平台、警务数据中心建设等项目。',
  },
  {
    q: '营商环境平台建设包括哪些内容？',
    a: '包括企业服务门户、政策精准推送、诉求反馈处理、数据分析决策等核心模块，助力政府优化营商环境。',
  },
  {
    q: '你们的技术团队有政府项目经验吗？',
    a: '核心团队拥有10年以上政务信息化和警务大数据项目经验，具备从需求调研、系统设计到交付运维的全流程服务能力。',
  },
  {
    q: '如何联系我们咨询合作？',
    a: '您可以通过电话 13699285133 联系我们的销售团队，或在官网预约咨询，我们会在第一时间与您对接。',
  },
]

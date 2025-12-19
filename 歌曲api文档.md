网易云音乐
来自网易云

GET
API链接
https://oiapi.net/api/Music_163
复制
请求参数
参数名	类型	必填	说明	示例
name	string	可选	歌曲名称	暂无
n	int	可选	歌曲序号	暂无
id	int	可选	歌曲id	暂无
返回参数
参数名	类型	说明
code	number	状态
message	string	提示信息
data	number	歌曲信息
返回示例
JSON
复制

								
{"code":0,"message":"获取成功","data":{"name":"月半小夜曲","picurl":"https://p2.music.126.net/SIFuIDfMNbuY9-IQcbTz5w==/109951166890517973.jpg","id":115162,"singers":[{"name":"李克勤","id":3699}],"url":"http://m704.music.126.net/20231108085844/47482c2707cbab092999ffb588c2aef5/jdymusic/obj/wo3DlMOGwrbDjj7DisKw/22259852012/96be/c8ed/7189/04fc664be8d965ecd401478c44323287.mp3?authSecret=0000018bac596fd11b310aa460bb1a98","pay":false}}

							
(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-331de626"],{"095f":function(t,e,a){"use strict";a.r(e);var n=function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"app-container"},[a("el-row",{staticStyle:{"flex-wrap":"wrap"},attrs:{type:"flex",gutter:20}},[a("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[a("el-table",{attrs:{data:t.data}},[a("el-table-column",{attrs:{prop:"id",label:"id"}}),a("el-table-column",{attrs:{label:"图标"},scopedSlots:t._u([{key:"default",fn:function(t){return[a("el-image",{staticStyle:{width:"40px",height:"40px"},attrs:{src:t.row.image}})]}}])}),a("el-table-column",{attrs:{label:"类型"},scopedSlots:t._u([{key:"default",fn:function(e){return[t._v(" "+t._s(e.row.type?"链接":"路径")+" ")]}}])}),a("el-table-column",{attrs:{prop:"name",label:"名称"}}),a("el-table-column",{attrs:{prop:"enable",label:"状态"}}),a("el-table-column",{attrs:{label:"操作",width:"200"},scopedSlots:t._u([{key:"default",fn:function(e){return[a("el-button",{attrs:{size:"mini",type:"primary"},on:{click:function(a){t.editorData=e.row}}},[t._v("编辑")]),a("el-button",{attrs:{size:"mini",type:"danger"},on:{click:function(a){return t.messageNotice(e.row.id)}}},[t._v("删除")])]}}])})],1)],1),a("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[a("el-form",{ref:"form",staticStyle:{padding:"20px"},attrs:{model:t.editorData}},[a("el-form-item",{attrs:{label:"图标","label-width":"80px"}},[a("el-upload",{attrs:{action:t.url+"/upload/full",beforeAvatarUpload:t.beforeAvatarUpload,"on-success":t.handleAvatarSuccess,headers:{Authorization:t.token},"show-file-list":!1}},[t.editorData.image?a("el-image",{staticStyle:{width:"80px",height:"80px"},attrs:{src:t.editorData.image,fit:"cover"}}):a("i",{staticClass:"el-icon-plus avatar-uploader-icon"})],1)],1),a("el-form-item",{attrs:{label:"路径","label-width":"80px"}},[a("el-input",{attrs:{placeholder:"根据类型填入信息 如/pages/user/sign"},model:{value:t.editorData.page,callback:function(e){t.$set(t.editorData,"page",e)},expression:"editorData.page"}})],1),a("el-form-item",{attrs:{label:"类型","label-width":"80px"}},[a("el-select",{model:{value:t.editorData.type,callback:function(e){t.$set(t.editorData,"type",e)},expression:"editorData.type"}},[a("el-option",{attrs:{label:"路径",value:0}}),a("el-option",{attrs:{label:"链接",value:1}})],1)],1),a("el-form-item",{attrs:{label:"名称","label-width":"80px"}},[a("el-input",{attrs:{plcaeholder:"建议填写4个字以内"},model:{value:t.editorData.name,callback:function(e){t.$set(t.editorData,"name",e)},expression:"editorData.name"}})],1),a("el-form-item",{attrs:{label:"状态","label-width":"80px"}},[a("el-select",{model:{value:t.editorData.enable,callback:function(e){t.$set(t.editorData,"enable",e)},expression:"editorData.enable"}},[a("el-option",{attrs:{label:"关闭",value:0}}),a("el-option",{attrs:{label:"启用",value:1}})],1)],1),a("el-form-item",{attrs:{"label-width":"80px"}},[a("el-button",{attrs:{type:"primary"},on:{click:function(e){return t.save()}}},[t._v("保存")])],1)],1)],1)],1)],1)},r=[],l=a("c7eb"),s=a("1da1"),o=(a("d3b7"),a("8593")),i=a("5f87"),c={data:function(){return{data:[],page:1,editorData:{id:null,page:"",name:"",image:"https://",type:0,enable:1},url:null,token:Object(i["a"])()}},created:function(){this.getData(),this.url="/"},methods:{getData:function(){var t=this;Object(o["a"])({page:this.page}).then((function(e){t.data=e.data.data}))},handleAvatarSuccess:function(t,e){this.editorData.image=t.data.url,console.log(t)},beforeAvatarUpload:function(t){var e=t.size/1024/1024<2;return e||this.$message.error("上传头像图片大小不能超过 2MB!"),e},save:function(){var t=this;null!=this.editorData.id?this.update():Object(o["b"])(this.editorData).then((function(e){t.$message({type:"success",message:"添加完成"}),t.getData(),t.resetData()}))},update:function(){var t=this;Object(o["d"])(this.editorData).then((function(e){t.$message({type:"success",message:"修改完成"}),console.log(e),t.getData(),t.resetData()}))},messageNotice:function(t){var e=this;return Object(s["a"])(Object(l["a"])().mark((function a(){return Object(l["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:e.$confirm("是否删除该页面？","提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then(Object(s["a"])(Object(l["a"])().mark((function a(){return Object(l["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:return a.next=2,e.deletePage(t);case 2:e.getData(),e.$message({type:"success",message:"删除成功"});case 4:case"end":return a.stop()}}),a)}))));case 1:case"end":return a.stop()}}),a)})))()},deletePage:function(t){return new Promise((function(e,a){Object(o["c"])({id:t}).then((function(t){e()}))}))},resetData:function(){this.editorData={id:null,page:"",name:"",image:"",type:0,enable:1}}}},u=c,p=a("2877"),d=Object(p["a"])(u,n,r,!1,null,null,null);e["default"]=d.exports},8593:function(t,e,a){"use strict";a.d(e,"h",(function(){return l})),a.d(e,"g",(function(){return s})),a.d(e,"i",(function(){return o})),a.d(e,"f",(function(){return i})),a.d(e,"j",(function(){return c})),a.d(e,"k",(function(){return u})),a.d(e,"e",(function(){return p})),a.d(e,"l",(function(){return d})),a.d(e,"a",(function(){return m})),a.d(e,"b",(function(){return b})),a.d(e,"d",(function(){return f})),a.d(e,"c",(function(){return h}));var n=a("5530"),r=a("b775");function l(t){return Object(r["a"])({url:"/install/check",method:"get",params:t})}function s(t){return Object(r["a"])({url:"/install/install",method:"post",params:Object(n["a"])({},t)})}function o(t){return Object(r["a"])({url:"install/newInstall",method:"post",params:Object(n["a"])(Object(n["a"])({},t),{},{webkey:"123456"})})}function i(t){return Object(r["a"])({url:"/system/getApiConfig",method:"get",params:Object(n["a"])(Object(n["a"])({},t),{},{webkey:"123456"})})}function c(t){return Object(r["a"])({url:"/system/apiConfigUpdate",method:"post",params:{params:t,webkey:"123456"}})}function u(t){return Object(r["a"])({url:"/system/setupEmail",method:"post",params:{params:t,webkey:"123456"}})}function p(){return Object(r["a"])({url:"/system/app",method:"post",params:{}})}function d(t){return Object(r["a"])({url:"/system/update",method:"post",params:{params:t,webkey:"123456"}})}function m(t){return Object(r["a"])({url:"/system/appHomepage",method:"get",params:Object(n["a"])({},t)})}function b(t){return Object(r["a"])({url:"/system/appHomepageAdd",method:"post",params:Object(n["a"])({},t)})}function f(t){return Object(r["a"])({url:"/system/appHomepageUpdate",method:"post",params:Object(n["a"])({},t)})}function h(t){return Object(r["a"])({url:"/system/appHomepageDelete",method:"post",params:Object(n["a"])({},t)})}}}]);
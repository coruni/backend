(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-df091276"],{"857a":function(t,e,a){var r=a("1d80"),l=/"/g;t.exports=function(t,e,a,n){var o=String(r(t)),i="<"+e;return""!==a&&(i+=" "+a+'="'+String(n).replace(l,"&quot;")+'"'),i+">"+o+"</"+e+">"}},9911:function(t,e,a){"use strict";var r=a("23e7"),l=a("857a"),n=a("af03");r({target:"String",proto:!0,forced:n("link")},{link:function(t){return l(this,"a","href",t)}})},af03:function(t,e,a){var r=a("d039");t.exports=function(t){return r((function(){var e=""[t]('"');return e!==e.toLowerCase()||e.split('"').length>3}))}},ceab:function(t,e,a){"use strict";a.r(e);var r=function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"app-container"},[a("el-row",{staticStyle:{"flex-wrap":"wrap"},attrs:{type:"flex",justify:"space-between",gutter:20}},[a("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[a("el-table",{staticStyle:{width:"100%",height:"600px"},attrs:{data:t.list.data,"max-height":"720"}},[a("el-table-column",{attrs:{label:"ID",prop:"id"}}),a("el-table-column",{attrs:{label:"名称",prop:"name"}}),a("el-table-column",{attrs:{label:"头像框",prop:"link"},scopedSlots:t._u([{key:"default",fn:function(t){return[a("el-image",{staticStyle:{width:"50px",height:"50px"},attrs:{src:t.row.link,fit:"fit"}})]}}])}),a("el-table-column",{attrs:{label:"创建者",prop:"creator"}}),a("el-table-column",{attrs:{label:"类型",prop:"type"},scopedSlots:t._u([{key:"default",fn:function(e){return[a("div",[t._v(t._s(0==e.row.type?"私人":"公开"))])]}}])}),a("el-table-column",{attrs:{label:"类型",prop:"permission"},scopedSlots:t._u([{key:"default",fn:function(e){return[a("div",[t._v(t._s(0==e.row.permission?"公开":"需要"))])]}}])}),a("el-table-column",{attrs:{label:"操作",width:"180"},scopedSlots:t._u([{key:"default",fn:function(e){return[a("el-button",{attrs:{type:"primary",size:"mini"}},[t._v("编辑")]),a("el-button",{attrs:{type:"danger",size:"mini"},on:{click:function(a){return t.messageNotice(e.row.id)}}},[t._v("删除")])]}}])})],1),a("el-pagination",{attrs:{background:"","page-count":t.list.count,layout:"prev, pager, next",total:t.list.total},on:{"prev-click":function(e){t.page+=1,t.getData()},"next-click":function(e){t.page-=1,t.getData()},"current-change":function(e){t.page=e,t.getData()}}})],1),a("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[a("el-form",{ref:"form",staticClass:"app-container",model:{value:t.form,callback:function(e){t.form=e},expression:"form"}},[a("el-form-item",{attrs:{label:"头像","label-width":"50px"}},[a("el-upload",{attrs:{action:"/upload/full",beforeAvatarUpload:t.beforeAvatarUpload,"on-success":t.handleAvatarSuccess,headers:{Authorization:t.getToken()},"show-file-list":!1}},[t.form.link?a("el-image",{staticStyle:{width:"80px",height:"80px"},attrs:{src:t.form.link}}):a("i",{staticClass:"el-icon-plus avatar-uploader-icon"})],1)],1),a("el-form-item",{attrs:{label:"名称","label-width":"50px"}},[a("el-input",{model:{value:t.form.name,callback:function(e){t.$set(t.form,"name",e)},expression:"form.name"}})],1),a("el-form-item",{attrs:{label:"类型","label-width":"50px"}},[a("el-select",{attrs:{placeholder:"公开或者私人"},model:{value:t.form.type,callback:function(e){t.$set(t.form,"type",e)},expression:"form.type"}},[a("el-option",{attrs:{value:0,label:"私人"}}),a("el-option",{attrs:{value:1,label:"公开"}})],1)],1),a("el-form-item",{attrs:{label:"权限","label-width":"50px"}},[a("el-select",{attrs:{placeholder:"需要或不需要"},model:{value:t.form.permission,callback:function(e){t.$set(t.form,"permission",e)},expression:"form.permission"}},[a("el-option",{attrs:{value:0,label:"不需要"}}),a("el-option",{attrs:{value:1,label:"需要"}})],1)],1),a("el-form-item",{attrs:{label:"状态","label-width":"50px"}},[a("el-select",{attrs:{placeholder:"关闭或开启"},model:{value:t.form.type,callback:function(e){t.$set(t.form,"type",e)},expression:"form.type"}},[a("el-option",{attrs:{value:0,label:"停用"}}),a("el-option",{attrs:{value:1,label:"启用"}})],1)],1),a("el-form-item",[a("el-button",{attrs:{type:"primary"},on:{click:function(e){return t.save()}}},[t._v("添加")])],1)],1)],1)],1)],1)},l=[],n=a("c7eb"),o=a("1da1"),i=(a("d3b7"),a("9911"),a("b775"));function s(t){return Object(i["a"])({url:"/headpicture/list",method:"get",params:t})}function c(t){return Object(i["a"])({url:"/headpicture/delete",method:"post",params:t})}function u(t){return Object(i["a"])({url:"/headpicture/add",method:"post",params:t})}var p=a("5f87"),f={data:function(){return{url:null,list:[],page:1,limit:10,form:{name:"",link:"",type:1,permission:0}}},created:function(){this.getData(),this.url="/"},methods:{getToken:p["a"],getData:function(){var t=this,e={page:this.page,limit:this.limit};s(e).then((function(e){t.list=e.data}))},deleteHead:function(t){return new Promise((function(e,a){c({id:t}).then((function(t){e()}))}))},save:function(){var t=this;null!=this.form.link&&this.form.link&&u(this.form).then((function(e){t.$message({message:"添加成功",type:"success"}),t.resetForm()}))},resetForm:function(){this.form={name:"",link:"",type:1,permission:0}},messageNotice:function(t){var e=this;return Object(o["a"])(Object(n["a"])().mark((function a(){return Object(n["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:e.$confirm("是否删除该头像框","提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then(Object(o["a"])(Object(n["a"])().mark((function a(){return Object(n["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:return a.next=2,e.deleteHead(t);case 2:e.getData(),e.$message({type:"success",message:"删除成功"});case 4:case"end":return a.stop()}}),a)}))));case 1:case"end":return a.stop()}}),a)})))()},handleAvatarSuccess:function(t,e){this.form.link=t.data.url},beforeAvatarUpload:function(t){var e=t.size/1024/1024<2;return e||this.$message.error("上传头像图片大小不能超过 2MB!"),e}}},m=f,d=a("2877"),b=Object(d["a"])(m,r,l,!1,null,null,null);e["default"]=b.exports}}]);
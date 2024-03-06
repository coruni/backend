(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-1dbe3a8c"],{"29c1":function(t,e,r){"use strict";r.r(e);var a=function(){var t=this,e=t.$createElement,r=t._self._c||e;return r("div",{staticClass:"app-container"},[r("el-row",{staticStyle:{"flex-wrap":"wrap"},attrs:{type:"flex",justify:"space-between",gutter:20}},[r("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[r("el-table",{staticStyle:{width:"100%"},attrs:{data:t.category}},[r("el-table-column",{attrs:{prop:"mid",label:"ID",width:"60"}}),r("el-table-column",{attrs:{label:"头像"},scopedSlots:t._u([{key:"default",fn:function(t){return[r("el-image",{staticStyle:{width:"40px",height:"40px"},attrs:{src:t.row.imgurl}})]}}])}),r("el-table-column",{attrs:{prop:"name",label:"分类名"}}),r("el-table-column",{attrs:{prop:"description",label:"介绍"}}),r("el-table-column",{attrs:{label:"操作",width:"200"},scopedSlots:t._u([{key:"default",fn:function(e){return[r("el-button",{attrs:{size:"mini",type:"primary"},on:{click:function(r){return t.editTap(e.row)}}},[t._v("编辑")]),r("el-button",{attrs:{size:"mini",type:"danger"},on:{click:function(r){return t.messageNotice(e.row.mid)}}},[t._v("删除")])]}}])})],1)],1),r("el-col",{attrs:{xs:24,sm:24,md:24,lg:12}},[r("el-form",{ref:"form",staticClass:"app-container",model:{value:t.form,callback:function(e){t.form=e},expression:"form"}},[r("el-form-item",{attrs:{label:"头像"}},[r("el-upload",{attrs:{action:t.url+"/upload/full",beforeAvatarUpload:t.beforeAvatarUpload,"on-success":t.handleAvatarSuccess,headers:{Authorization:t.getToken()},"show-file-list":!1}},[t.form.imgurl?r("el-image",{staticStyle:{width:"80px",height:"80px"},attrs:{src:t.form.imgurl}}):r("i",{staticClass:"el-icon-plus avatar-uploader-icon"})],1)],1),r("el-form-item",{attrs:{label:"名称"}},[r("el-input",{model:{value:t.form.name,callback:function(e){t.$set(t.form,"name",e)},expression:"form.name"}})],1),r("el-form-item",{attrs:{label:"介绍"}},[r("el-input",{model:{value:t.form.description,callback:function(e){t.$set(t.form,"description",e)},expression:"form.description"}})],1),r("label",[t._v("颜色设置")]),r("el-form-item",{attrs:{label:"主题色"}},[r("el-input",{model:{value:t.form.opt.primary,callback:function(e){t.$set(t.form.opt,"primary",e)},expression:"form.opt.primary"}})],1),r("el-form-item",{attrs:{label:"下划线"}},[r("el-input",{model:{value:t.form.opt.underline,callback:function(e){t.$set(t.form.opt,"underline",e)},expression:"form.opt.underline"}})],1),r("el-form-item",{attrs:{label:"文字颜色"}},[r("el-input",{model:{value:t.form.opt.color,callback:function(e){t.$set(t.form.opt,"color",e)},expression:"form.opt.color"}})],1),r("el-form-item",[r("el-button",{attrs:{type:"primary"},on:{click:function(e){t.form&&t.form.mid?t.update():t.newCategory()}}},[t._v("提交")])],1)],1)],1)],1)],1)},n=[],o=r("c7eb"),i=r("1da1"),s=(r("e9c4"),r("b0c0"),r("a4d3"),r("e01a"),r("d3b7"),r("c405")),c=r("5f87"),l={data:function(){return{form:{mid:0,name:"",description:"",imgurl:"",iswaterfall:0,isrecommend:0,opt:{background:"",color:"",primary:"",underline:""}},category:[],page:1,limit:20,url:null}},created:function(){this.initData(),this.url="/"},methods:{getToken:c["a"],initData:function(){this.getCategory()},getCategory:function(){var t=this,e={page:this.page,limit:this.limit,params:JSON.stringify({type:"tag"})};Object(s["b"])(e).then((function(e){t.category=e.data.data}))},handleAvatarSuccess:function(t,e){this.form.imgurl=t.data.url},beforeAvatarUpload:function(t){var e=t.size/1024/1024<2;return e||this.$message.error("上传头像图片大小不能超过 2MB!"),e},update:function(){var t=this,e={id:this.form.mid,name:this.form.name,description:this.form.description,avatar:this.form.imgurl,opt:JSON.stringify(this.form.opt)};Object(s["e"])(e).then((function(e){t.$message({message:"修改成功",type:"success"}),t.resetForm()}))},resetForm:function(){this.form={name:"",description:"",imgurl:"",opt:{background:"",color:"",primary:"",underline:""}}},deleteCategory:function(t){return new Promise((function(e,r){Object(s["c"])({id:t}).then((function(t){e()}))}))},messageNotice:function(t){var e=this;return Object(i["a"])(Object(o["a"])().mark((function r(){return Object(o["a"])().wrap((function(r){while(1)switch(r.prev=r.next){case 0:e.$confirm("是否删除该标签？","提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then(Object(i["a"])(Object(o["a"])().mark((function r(){return Object(o["a"])().wrap((function(r){while(1)switch(r.prev=r.next){case 0:return r.next=2,e.deleteCategory(t);case 2:e.getCategory(),e.$message({type:"success",message:"删除成功"});case 4:case"end":return r.stop()}}),r)}))));case 1:case"end":return r.stop()}}),r)})))()},editTap:function(t){t=t;t.hasOwnProperty("opt")||(t.opt={background:"",color:"",primary:"",underline:""}),this.form=t},newCategory:function(){var t=this;if(this.form.name){var e={name:this.form.name,type:"tag",description:this.form.description,opt:JSON.stringify(this.form.opt),avatar:this.form.imgurl};Object(s["d"])(e).then((function(e){200==e.code&&(t.$message({type:"success",message:"添加成功"}),t.getCategory(),t.resetForm())}))}},categoryAction:function(t,e){var r=this,a={type:t,id:e};Object(s["a"])(a).then((function(t){200==t.code&&(r.$message({type:"success",message:"设置成功"}),r.getCategory())}))}}},u=l,m=r("2877"),f=Object(m["a"])(u,a,n,!1,null,null,null);e["default"]=f.exports},c405:function(t,e,r){"use strict";r.d(e,"b",(function(){return n})),r.d(e,"e",(function(){return o})),r.d(e,"c",(function(){return i})),r.d(e,"d",(function(){return s})),r.d(e,"a",(function(){return c}));var a=r("b775");function n(t){return Object(a["a"])({url:"/category/list",method:"get",params:t})}function o(t){return Object(a["a"])({url:"/category/update",method:"post",params:t})}function i(t){return Object(a["a"])({url:"/category/delete",method:"post",params:t})}function s(t){return Object(a["a"])({url:"/category/add",method:"post",params:t})}function c(t){return Object(a["a"])({url:"/category/action",method:"post",params:t})}},e9c4:function(t,e,r){var a=r("23e7"),n=r("d066"),o=r("d039"),i=n("JSON","stringify"),s=/[\uD800-\uDFFF]/g,c=/^[\uD800-\uDBFF]$/,l=/^[\uDC00-\uDFFF]$/,u=function(t,e,r){var a=r.charAt(e-1),n=r.charAt(e+1);return c.test(t)&&!l.test(n)||l.test(t)&&!c.test(a)?"\\u"+t.charCodeAt(0).toString(16):t},m=o((function(){return'"\\udf06\\ud834"'!==i("\udf06\ud834")||'"\\udead"'!==i("\udead")}));i&&a({target:"JSON",stat:!0,forced:m},{stringify:function(t,e,r){var a=i.apply(null,arguments);return"string"==typeof a?a.replace(s,u):a}})}}]);
(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-2d0d3352"],{"5c7e":function(t,e,a){"use strict";a.r(e);var n=function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"app-container"},[a("el-table",{staticStyle:{width:"100%","border-radius":"5px"},attrs:{data:t.list.data,"max-height":"720",size:"mini",border:"","header-cell-style":{background:"#409EFF",color:"#ffffff"}}},[a("el-table-column",{attrs:{label:"ID",prop:"id"}}),a("el-table-column",{attrs:{label:"名称",prop:"name"}}),a("el-table-column",{attrs:{label:"头像",prop:"link"},scopedSlots:t._u([{key:"default",fn:function(t){return[a("el-image",{staticStyle:{width:"50px",height:"50px"},attrs:{src:t.row.pic,fit:"fit"}})]}}])}),a("el-table-column",{attrs:{label:"介绍",prop:"intro"}}),a("el-table-column",{attrs:{label:"操作",width:"180"},scopedSlots:t._u([{key:"default",fn:function(e){return[a("el-button",{attrs:{type:"primary",size:"mini"},on:{click:function(a){return t.onEdit(e.row)}}},[t._v("编辑")]),a("el-button",{attrs:{type:"danger",size:"mini"},on:{click:function(a){return t.messageNotice(e.row.id)}}},[t._v("删除")])]}}])})],1),a("el-pagination",{attrs:{background:"","page-count":t.list.count,layout:"prev, pager, next",total:t.list.total},on:{"prev-click":function(e){t.page+=1,t.getData()},"next-click":function(e){t.page-=1,t.getData()},"current-change":function(e){t.page=e,t.getData()}}}),a("el-dialog",{attrs:{title:"添加",visible:t.dialogVisible,width:"40%"},on:{"update:visible":function(e){t.dialogVisible=e}}},[a("el-form",{ref:"form",staticClass:"app-container",model:{value:t.form,callback:function(e){t.form=e},expression:"form"}},[a("el-form-item",{attrs:{label:"头像","label-width":"50px"}},[a("el-upload",{attrs:{action:"/upload/full",beforeAvatarUpload:t.beforeAvatarUpload,"on-success":t.handleAvatarSuccess,headers:{Authorization:t.getToken()},"show-file-list":!1}},[t.form.pic?a("el-image",{staticStyle:{width:"80px",height:"80px"},attrs:{src:t.form.pic}}):a("i",{staticClass:"el-icon-plus avatar-uploader-icon"})],1)],1),a("el-form-item",{attrs:{label:"名称","label-width":"50px"}},[a("el-input",{model:{value:t.form.name,callback:function(e){t.$set(t.form,"name",e)},expression:"form.name"}})],1),a("el-form-item",{attrs:{label:"介绍","label-width":"50px"}},[a("el-input",{model:{value:t.form.intro,callback:function(e){t.$set(t.form,"intro",e)},expression:"form.intro"}})],1),a("el-form-item",[a("el-button",{attrs:{type:"primary"},on:{click:function(e){t.form.id?t.update():t.save()}}},[t._v(t._s(t.form.id?"更新":"添加"))])],1)],1)],1)],1)},i=[],r=a("c7eb"),o=a("1da1"),s=a("5530"),l=(a("d3b7"),a("b775"));function c(t){return Object(l["a"])({url:"/shop/typeList",method:"get",params:t})}function u(t){return Object(l["a"])({url:"/shop/typeAdd",method:"post",params:t})}function p(t){return Object(l["a"])({url:"/shop/typeEdit",method:"post",params:t})}function f(t){return Object(l["a"])({url:"/shop/typeDelete",method:"post",params:t})}var m=a("5f87"),d={data:function(){return{list:{},page:1,limit:10,form:{id:0,name:"",pic:"",intro:"",parent:0},url:null,dialogVisible:!1}},created:function(){this.getData(),this.url="/"},methods:{onEdit:function(t){this.form=Object(s["a"])({},t),this.dialogVisible=!0},getToken:m["a"],getData:function(){var t=this,e={page:this.page,limit:this.limit};c(e).then((function(e){t.list=e.data}))},save:function(){var t=this,e=this.form;u(e).then((function(e){t.$message({message:"添加成功",type:"success"}),t.getData(),t.resetForm()})),this.dialogVisible=!1},handleAvatarSuccess:function(t,e){this.form.pic=t.data.url},beforeAvatarUpload:function(t){var e=t.size/1024/1024<2;return e||this.$message.error("上传头像图片大小不能超过 2MB!"),e},deleteCategory:function(t){return new Promise((function(e,a){f({id:t}).then((function(t){e()}))}))},messageNotice:function(t){var e=this;return Object(o["a"])(Object(r["a"])().mark((function a(){return Object(r["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:e.$confirm("是否删除该头像框","提示",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"}).then(Object(o["a"])(Object(r["a"])().mark((function a(){return Object(r["a"])().wrap((function(a){while(1)switch(a.prev=a.next){case 0:return a.next=2,e.deleteCategory(t);case 2:e.getData(),e.$message({type:"success",message:"删除成功"});case 4:case"end":return a.stop()}}),a)}))));case 1:case"end":return a.stop()}}),a)})))()},update:function(){var t=this,e=this.form;p(e).then((function(e){t.$message({type:"success",message:"更新成功"}),t.resetForm,t.getData()}))},resetForm:function(){this.form={id:0,name:"",pic:"",intro:"",parent:0}}}},h=d,b=a("2877"),g=Object(b["a"])(h,n,i,!1,null,null,null);e["default"]=g.exports}}]);
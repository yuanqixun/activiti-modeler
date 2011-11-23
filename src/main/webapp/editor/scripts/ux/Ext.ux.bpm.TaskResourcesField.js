Ext.namespace("Ext.ux.bpm");

String.prototype.format = function() {
  var str = this;
  var matchs = str.match(/\{\d\}/ig);
  for (var i = 0; i < arguments.length; i++) {
    str = str.replace(eval('/\\{' + i + '\\}/ig'), arguments[i]);
  }
  return str;
};

Ext.ux.bpm.TaskResourcesField = function(config, items, key, facade){
   Ext.ux.bpm.TaskResourcesField.superclass.constructor.call(this, config);
  this.items  = items;
  this.key  = key;
  this.facade = facade;
  this.rootCompanyUrl = "http://192.168.28.3:8080/web/restapi/org/companys";
  this.subCompanyUrl = "http://192.168.28.3:8080/web/restapi/org/company/{0}/subcompany";
  this.companyDeptUrl = "http://192.168.28.3:8080/web/restapi/org/company/{0}/department";
  this.companyChildrenUrl = "http://192.168.28.3:8080/web/restapi/org/company/{0}/chidren";
};

/**
 * This is a special trigger field used for complex properties.
 * The trigger field opens a dialog that shows a list of properties.
 * The entered values will be stored as trigger field value in the JSON format.
 */
Ext.extend(Ext.ux.bpm.TaskResourcesField, Ext.form.TriggerField,  {
  /**
     * @cfg {String} triggerClass
     * An additional CSS class used to style the trigger button.  The trigger will always get the
     * class 'x-form-trigger' and triggerClass will be <b>appended</b> if specified.
     */
    triggerClass: 'x-form-complex-trigger',
  readOnly:   false,
  emptyText:    ORYX.I18N.PropertyWindow.clickIcon,
  loadTreeNode: function(node) {
  var dataUrl;
  if (node.firstChild.text == 'loading') {
    var id = node.id;
    var type = node.type;
    if (id == 'root')
      dataUrl = this.rootCompanyUrl;
    else {
      if (type == "C") {
        dataUrl = this.companyChildrenUrl.format(id);
      }
    }
    var store = new Ext.data.Store({
          autoLoad : true,
          proxy : new Ext.data.ScriptTagProxy({
                url : dataUrl
              }),
          reader : new Ext.data.JsonReader({
                root : 'data',
                totalProperty : 'totalCount',
                idProperty : 'id',
                fields : ['id', 'text', 'children', 'expanded',
                    'leaf', 'type']
              }),
          listeners : {
            load : function(a, recs) {
              var data = a.reader.jsonData.data;
              if (data) {
              	node.firstChild.remove();
              	for(var i=0;i<data.length;i++){
                var cnode = new Ext.tree.AsyncTreeNode({
                      id : data[i].id,
                      text : data[i].text,
                      title:data[i].text,
                      children : [{
                            text : 'loading',
                            leaf : true
                          }]
                    });
                cnode.type = data[i].type;
                cnode.on("expand", this.loadTreeNode.bind(this));
                node.appendChild(cnode);
              	}
              }
            }.bind(this)
          }
        });
  }
},
    
  /**
   * Builds the JSON value from the data source of the grid in the dialog.
   */
  buildValue2: function() {
    var ds = this.grid.getStore();
    ds.commitChanges();
    
    if (ds.getCount() == 0) {
      return "";
    }
    
    var jsonString = "[";
    for (var i = 0; i < ds.getCount(); i++) {
      var data = ds.getAt(i);   
      jsonString += "{";  
      for (var j = 0; j < this.items.length; j++) {
        var key = this.items[j].id();
        jsonString += key + ':' + ("" + data.get(key)).toJSON();
        if (j < (this.items.length - 1)) {
          jsonString += ", ";
        }
      }
      jsonString += "}";
      if (i < (ds.getCount() - 1)) {
        jsonString += ", ";
      }
    }
    jsonString += "]";
    
    jsonString = "{'totalCount':" + ds.getCount().toJSON() + 
      ", 'items':" + jsonString + "}";
    return Object.toJSON(jsonString.evalJSON());
  },
  
  /**
   * Returns the field key.
   */
  getFieldKey: function() {
    return this.key;
  },
  
  /**
   * Returns the actual value of the trigger field.
   * If the table does not contain any values the empty
   * string will be returned.
   */
    getValue2 : function(){
    // return actual value if grid is active
    if (this.grid) {
      return this.buildValue();     
    } else if (this.data == undefined) {
      return "";
    } else {
      return this.data;
    }
    },
  
  /**
   * Sets the value of the trigger field.
   * In this case this sets the data that will be shown in
   * the grid of the dialog.
   * 
   * @param {Object} value The value to be set (JSON format or empty string)
   */
  setValue: function(value) { 
    if (value.length > 0) {
      // set only if this.data not set yet
      // only to initialize the grid
      if (this.data == undefined) {
        this.data = value;
      }
    }
  },
  
  /**
   * Returns false. In this way key events will not be propagated
   * to other elements.
   * 
   * @param {Object} event The keydown event.
   */
  keydownHandler: function(event) {
    return false;
  },
  
  /**
   * The listeners of the dialog. 
   * 
   * If the dialog is hidded, a dialogClosed event will be fired.
   * This has to be used by the parent element of the trigger field
   * to reenable the trigger field (focus gets lost when entering values
   * in the dialog).
   */
    dialogListeners : {
        show : function(){ // retain focus styling
            this.onFocus(); 
      this.facade.registerOnEvent(ORYX.CONFIG.EVENT_KEYDOWN, this.keydownHandler.bind(this));
      this.facade.disableEvent(ORYX.CONFIG.EVENT_KEYDOWN);
      return;
        },
        hide : function(){

            var dl = this.dialogListeners;
            this.dialog.un("show", dl.show,  this);
            this.dialog.un("hide", dl.hide,  this);
      
      this.dialog.destroy(true);
      this.grid.destroy(true);
      delete this.grid;
      delete this.dialog;
      
      this.facade.unregisterOnEvent(ORYX.CONFIG.EVENT_KEYDOWN, this.keydownHandler.bind(this));
      this.facade.enableEvent(ORYX.CONFIG.EVENT_KEYDOWN);
      
      // store data and notify parent about the closed dialog
      // parent has to handel this event and start editing the text field again
      this.fireEvent('dialogClosed', this.data);
      
     Ext.ux.bpm.TaskResourcesField.superclass.setValue.call(this, this.data);
        }
    },  
  
  /**
   * Builds up the initial values of the grid.
   * 
   * @param {Object} recordType The record type of the grid.
   * @param {Object} items      The initial items of the grid (columns)
   */
  buildInitial: function(recordType, items) {
    var initial = new Hash();
    
    for (var i = 0; i < items.length; i++) {
      var id = items[i].id();
      initial[id] = items[i].value();
    }
    
    var RecordTemplate = Ext.data.Record.create(recordType);
    return new RecordTemplate(initial);
  },
  
  /**
   * Builds up the column model of the grid. The parent element of the
   * grid.
   * 
   * Sets up the editors for the grid columns depending on the 
   * type of the items.
   * 
   * @param {Object} parent The 
   */
  buildColumnModel: function(parent) {
    var cols = [];
    for (var i = 0; i < this.items.length; i++) {
      var id    = this.items[i].id();
      var header  = this.items[i].name();
      var width   = this.items[i].width();
      var type  = this.items[i].type();
      var editor;
      
      if (type == ORYX.CONFIG.TYPE_STRING) {
        editor = new Ext.form.TextField({ allowBlank : this.items[i].optional(), width : width});
      } else if (type == ORYX.CONFIG.TYPE_CHOICE) {       
        var items = this.items[i].items();
        var select = ORYX.Editor.graft("http://www.w3.org/1999/xhtml", parent, ['select', {style:'display:none'}]);
        var optionTmpl = new Ext.Template('<option value="{value}">{value}</option>');
        items.each(function(value){ 
          optionTmpl.append(select, {value:value.value()}); 
        });       
        
        editor = new Ext.form.ComboBox(
          { typeAhead: true, triggerAction: 'all', transform:select, lazyRender:true,  msgTarget:'title', width : width});      
      } else if (type == ORYX.CONFIG.TYPE_BOOLEAN) {
        editor = new Ext.form.Checkbox( { width : width } );
      }
          
      cols.push({
        id:     id,
        header:   header,
        dataIndex:  id,
        resizable:  true,
        editor:   editor,
        width:    width
          });
      
    }
    return new Ext.grid.ColumnModel(cols);
  },
  
  /**
   * After a cell was edited the changes will be commited.
   * 
   * @param {Object} option The option that was edited.
   */
  afterEdit: function(option) {
    option.grid.getStore().commitChanges();
  },
    
  /**
   * Before a cell is edited it has to be checked if this 
   * cell is disabled by another cell value. If so, the cell editor will
   * be disabled.
   * 
   * @param {Object} option The option to be edited.
   */
  beforeEdit: function(option) {

    var state = this.grid.getView().getScrollState();
    
    var col = option.column;
    var row = option.row;
    var editId = this.grid.getColumnModel().config[col].id;
    // check if there is an item in the row, that disables this cell
    for (var i = 0; i < this.items.length; i++) {
      // check each item that defines a "disable" property
      var item = this.items[i];
      var disables = item.disable();
      if (disables != undefined) {
        
        // check if the value of the column of this item in this row is equal to a disabling value
        var value = this.grid.getStore().getAt(row).get(item.id());
        for (var j = 0; j < disables.length; j++) {
          var disable = disables[j];
          if (disable.value == value) {
            
            for (var k = 0; k < disable.items.length; k++) {
              // check if this value disables the cell to select 
              // (id is equals to the id of the column to edit)
              var disItem = disable.items[k];
              if (disItem == editId) {
                this.grid.getColumnModel().getCellEditor(col, row).disable();
                return;
              }
            }
          }
        }   
      }
    }
    this.grid.getColumnModel().getCellEditor(col, row).enable();
    //this.grid.getView().restoreScroll(state);
  },
  
    /**
     * If the trigger was clicked a dialog has to be opened
     * to enter the values for the complex property.
     */
    onTriggerClick : function(){
        if(this.disabled){
            return;
        }
        
    var myData = [
        ['袁启勋','138****4638','yuanqixun@eontime.co.cn']
       ];

    // create the data store
    var store = new Ext.data.SimpleStore({
          fields : [{
                name : 'name'
              }, {
                name : 'phone'
              }, {
                name : 'email'
              }
              ]
        });
    store.loadData(myData);

    // create the Grid
    var employe = new Ext.grid.GridPanel({
          store : store,
          region : "center",
          columns : [{
                header : "姓名",
                width : 160,
                sortable : true,
                dataIndex : 'name'
              }, {
                header : "电话",
                width : 75,
                dataIndex : 'phone'
              }, {
                header : "邮箱",
                width : 75,
                dataIndex : 'email'
              }
          ],
          stripeRows : true,
          height : 350,
          width : 600
//          title : 'Array Grid'
        });

        var root = new Ext.tree.AsyncTreeNode({
            text : 'root',
            id : 'root',
            expanded : false,
            children : [{
                  text : 'loading',
                  iconCls : 'loading',
                  leaf : true
                }]
          });
      root.on("expand", this.loadTreeNode.bind(this));
       
      var treeLoader = new Ext.tree.TreeLoader();
      var tree = new Ext.tree.TreePanel({
      	region : "west",
          width : 200,
            root : root,
            loader : treeLoader,
            rootVisible : true
          });

    var south = new Ext.Panel({
          region : 'south',
          split : true,
          height : 100
        });

    this.dialog = new Ext.Window({
          title : '任务执行人选择对话框',
          closable : true,
          modal:true,
          width : 600,
          height : 450,
          layout : 'border',
          items : [tree,employe, south],
          buttons : [{
                text : '确定',
                disabled : true
              }, {
                text : '取消',
                handler : function() {
                  this.dialog.hide();
                },
                  scope:this
              }]
        });
this.dialog.show(); 
  }
});
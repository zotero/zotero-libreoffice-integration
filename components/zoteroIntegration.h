/*
 * DO NOT EDIT.  THIS FILE IS GENERATED FROM /Users/simon/Desktop/Development/FS/zotero/extension/trunk/idl/zoteroIntegration.idl
 */

#ifndef __gen_zoteroIntegration_h__
#define __gen_zoteroIntegration_h__


#ifndef __gen_nsISupports_h__
#include "nsISupports.h"
#endif

#ifndef __gen_nsISimpleEnumerator_h__
#include "nsISimpleEnumerator.h"
#endif

/* For IDL files that don't want to include root IDL files. */
#ifndef NS_NO_VTABLE
#define NS_NO_VTABLE
#endif

/* starting interface:    zoteroIntegrationField */
#define ZOTEROINTEGRATIONFIELD_IID_STR "aedb37a0-48bb-11de-8a39-0800200c9a66"

#define ZOTEROINTEGRATIONFIELD_IID \
  {0xaedb37a0, 0x48bb, 0x11de, \
    { 0x8a, 0x39, 0x08, 0x00, 0x20, 0x0c, 0x9a, 0x66 }}

class NS_NO_VTABLE NS_SCRIPTABLE zoteroIntegrationField : public nsISupports {
 public: 

  NS_DECLARE_STATIC_IID_ACCESSOR(ZOTEROINTEGRATIONFIELD_IID)

  /**
   * Deletes this field and its contents.
   */
  /* void delete (); */
  NS_SCRIPTABLE NS_IMETHOD Delete(void) = 0;

  /**
   * Selects this field.
   */
  /* void select (); */
  NS_SCRIPTABLE NS_IMETHOD Select(void) = 0;

  /**
   * Removes this field, but maintains the field's contents.
   */
  /* void removeCode (); */
  NS_SCRIPTABLE NS_IMETHOD RemoveCode(void) = 0;

  /**
   * Sets the text inside this field to a specified plain text string or pseudo-RTF formatted text
   * string.
   */
  /* void setText (in wstring text, in boolean isRich); */
  NS_SCRIPTABLE NS_IMETHOD SetText(const PRUnichar *text, PRBool isRich) = 0;

  /**
   * This field's code.
   */
  /* wstring getCode (); */
  NS_SCRIPTABLE NS_IMETHOD GetCode(PRUnichar **_retval) = 0;

  /* void setCode (in wstring code); */
  NS_SCRIPTABLE NS_IMETHOD SetCode(const PRUnichar *code) = 0;

  /**
   * This field's note index, if it is in a footnote or endnote; otherwise zero.
   */
  /* unsigned long getNoteIndex (); */
  NS_SCRIPTABLE NS_IMETHOD GetNoteIndex(PRUint32 *_retval) = 0;

  /**
   * Returns true if this field and the passed field are actually references to the same field.
   */
  /* boolean equals (in zoteroIntegrationField field); */
  NS_SCRIPTABLE NS_IMETHOD Equals(zoteroIntegrationField *field, PRBool *_retval) = 0;

};

  NS_DEFINE_STATIC_IID_ACCESSOR(zoteroIntegrationField, ZOTEROINTEGRATIONFIELD_IID)

/* Use this macro when declaring classes that implement this interface. */
#define NS_DECL_ZOTEROINTEGRATIONFIELD \
  NS_SCRIPTABLE NS_IMETHOD Delete(void); \
  NS_SCRIPTABLE NS_IMETHOD Select(void); \
  NS_SCRIPTABLE NS_IMETHOD RemoveCode(void); \
  NS_SCRIPTABLE NS_IMETHOD SetText(const PRUnichar *text, PRBool isRich); \
  NS_SCRIPTABLE NS_IMETHOD GetCode(PRUnichar **_retval); \
  NS_SCRIPTABLE NS_IMETHOD SetCode(const PRUnichar *code); \
  NS_SCRIPTABLE NS_IMETHOD GetNoteIndex(PRUint32 *_retval); \
  NS_SCRIPTABLE NS_IMETHOD Equals(zoteroIntegrationField *field, PRBool *_retval); 

/* Use this macro to declare functions that forward the behavior of this interface to another object. */
#define NS_FORWARD_ZOTEROINTEGRATIONFIELD(_to) \
  NS_SCRIPTABLE NS_IMETHOD Delete(void) { return _to Delete(); } \
  NS_SCRIPTABLE NS_IMETHOD Select(void) { return _to Select(); } \
  NS_SCRIPTABLE NS_IMETHOD RemoveCode(void) { return _to RemoveCode(); } \
  NS_SCRIPTABLE NS_IMETHOD SetText(const PRUnichar *text, PRBool isRich) { return _to SetText(text, isRich); } \
  NS_SCRIPTABLE NS_IMETHOD GetCode(PRUnichar **_retval) { return _to GetCode(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD SetCode(const PRUnichar *code) { return _to SetCode(code); } \
  NS_SCRIPTABLE NS_IMETHOD GetNoteIndex(PRUint32 *_retval) { return _to GetNoteIndex(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD Equals(zoteroIntegrationField *field, PRBool *_retval) { return _to Equals(field, _retval); } 

/* Use this macro to declare functions that forward the behavior of this interface to another object in a safe way. */
#define NS_FORWARD_SAFE_ZOTEROINTEGRATIONFIELD(_to) \
  NS_SCRIPTABLE NS_IMETHOD Delete(void) { return !_to ? NS_ERROR_NULL_POINTER : _to->Delete(); } \
  NS_SCRIPTABLE NS_IMETHOD Select(void) { return !_to ? NS_ERROR_NULL_POINTER : _to->Select(); } \
  NS_SCRIPTABLE NS_IMETHOD RemoveCode(void) { return !_to ? NS_ERROR_NULL_POINTER : _to->RemoveCode(); } \
  NS_SCRIPTABLE NS_IMETHOD SetText(const PRUnichar *text, PRBool isRich) { return !_to ? NS_ERROR_NULL_POINTER : _to->SetText(text, isRich); } \
  NS_SCRIPTABLE NS_IMETHOD GetCode(PRUnichar **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetCode(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD SetCode(const PRUnichar *code) { return !_to ? NS_ERROR_NULL_POINTER : _to->SetCode(code); } \
  NS_SCRIPTABLE NS_IMETHOD GetNoteIndex(PRUint32 *_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetNoteIndex(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD Equals(zoteroIntegrationField *field, PRBool *_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->Equals(field, _retval); } 

#if 0
/* Use the code below as a template for the implementation class for this interface. */

/* Header file */
class _MYCLASS_ : public zoteroIntegrationField
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_ZOTEROINTEGRATIONFIELD

  _MYCLASS_();

private:
  ~_MYCLASS_();

protected:
  /* additional members */
};

/* Implementation file */
NS_IMPL_ISUPPORTS1(_MYCLASS_, zoteroIntegrationField)

_MYCLASS_::_MYCLASS_()
{
  /* member initializers and constructor code */
}

_MYCLASS_::~_MYCLASS_()
{
  /* destructor code */
}

/* void delete (); */
NS_IMETHODIMP _MYCLASS_::Delete()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void select (); */
NS_IMETHODIMP _MYCLASS_::Select()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void removeCode (); */
NS_IMETHODIMP _MYCLASS_::RemoveCode()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void setText (in wstring text, in boolean isRich); */
NS_IMETHODIMP _MYCLASS_::SetText(const PRUnichar *text, PRBool isRich)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* wstring getCode (); */
NS_IMETHODIMP _MYCLASS_::GetCode(PRUnichar **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void setCode (in wstring code); */
NS_IMETHODIMP _MYCLASS_::SetCode(const PRUnichar *code)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* unsigned long getNoteIndex (); */
NS_IMETHODIMP _MYCLASS_::GetNoteIndex(PRUint32 *_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* boolean equals (in zoteroIntegrationField field); */
NS_IMETHODIMP _MYCLASS_::Equals(zoteroIntegrationField *field, PRBool *_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* End of implementation class template. */
#endif


/* starting interface:    zoteroIntegrationDocument */
#define ZOTEROINTEGRATIONDOCUMENT_IID_STR "be1c5c1f-9ed2-4154-98fb-822d1fede569"

#define ZOTEROINTEGRATIONDOCUMENT_IID \
  {0xbe1c5c1f, 0x9ed2, 0x4154, \
    { 0x98, 0xfb, 0x82, 0x2d, 0x1f, 0xed, 0xe5, 0x69 }}

/**
 * The zoteroIntegrationDocument interface corresponds to a single word processing document.
 */
class NS_NO_VTABLE NS_SCRIPTABLE zoteroIntegrationDocument : public nsISupports {
 public: 

  NS_DECLARE_STATIC_IID_ACCESSOR(ZOTEROINTEGRATIONDOCUMENT_IID)

  /**
   * Displays a dialog in the word processing application
   */
  /* short displayAlert (in wstring dialogText, in unsigned short icon, in unsigned short buttons); */
  NS_SCRIPTABLE NS_IMETHOD DisplayAlert(const PRUnichar *dialogText, PRUint16 icon, PRUint16 buttons, PRInt16 *_retval) = 0;

  /**
   * Brings this document to the foreground (if necessary to return after displaying a dialog)
   */
  /* void activate (); */
  NS_SCRIPTABLE NS_IMETHOD Activate(void) = 0;

  /**
   * Determines whether a field can be inserted at the current position.
   */
  /* boolean canInsertField (in string fieldType); */
  NS_SCRIPTABLE NS_IMETHOD CanInsertField(const char *fieldType, PRBool *_retval) = 0;

  /**
   * Returns the field in which the cursor resides, or NULL if none.
   */
  /* zoteroIntegrationField cursorInField (in string fieldType); */
  NS_SCRIPTABLE NS_IMETHOD CursorInField(const char *fieldType, zoteroIntegrationField **_retval) = 0;

  /**
   * The document data property from the current document.
   */
  /* wstring getDocumentData (); */
  NS_SCRIPTABLE NS_IMETHOD GetDocumentData(PRUnichar **_retval) = 0;

  /* void setDocumentData (in wstring data); */
  NS_SCRIPTABLE NS_IMETHOD SetDocumentData(const PRUnichar *data) = 0;

  /**
   * Inserts a field at the given position and initializes the field object.
   */
  /* zoteroIntegrationField insertField (in string fieldType, in unsigned short noteType); */
  NS_SCRIPTABLE NS_IMETHOD InsertField(const char *fieldType, PRUint16 noteType, zoteroIntegrationField **_retval) = 0;

  /**
   * Inserts an uninitialized field object at the given position
   */
  /* nsISimpleEnumerator getFields (in string fieldType); */
  NS_SCRIPTABLE NS_IMETHOD GetFields(const char *fieldType, nsISimpleEnumerator **_retval) = 0;

  /**
   * Converts all fields in a document to a different fieldType or noteType
   */
  /* void convert (in nsISimpleEnumerator fields, in string toFieldType, [array, size_is (count)] in unsigned short toNoteType, in unsigned long count); */
  NS_SCRIPTABLE NS_IMETHOD Convert(nsISimpleEnumerator *fields, const char *toFieldType, PRUint16 *toNoteType, PRUint32 count) = 0;

  /**
   * Runs on function completion to clean up everything integration played with.
   */
  /* void cleanup (); */
  NS_SCRIPTABLE NS_IMETHOD Cleanup(void) = 0;

  enum { DIALOG_ICON_STOP = 0U };

  enum { DIALOG_ICON_NOTICE = 1U };

  enum { DIALOG_ICON_CAUTION = 2U };

  enum { DIALOG_BUTTONS_OK = 0U };

  enum { DIALOG_BUTTONS_OK_CANCEL = 1U };

  enum { DIALOG_BUTTONS_YES_NO = 2U };

  enum { DIALOG_BUTTONS_YES_NO_CANCEL = 3U };

  enum { NOTE_FOOTNOTE = 1U };

  enum { NOTE_ENDNOTE = 2U };

};

  NS_DEFINE_STATIC_IID_ACCESSOR(zoteroIntegrationDocument, ZOTEROINTEGRATIONDOCUMENT_IID)

/* Use this macro when declaring classes that implement this interface. */
#define NS_DECL_ZOTEROINTEGRATIONDOCUMENT \
  NS_SCRIPTABLE NS_IMETHOD DisplayAlert(const PRUnichar *dialogText, PRUint16 icon, PRUint16 buttons, PRInt16 *_retval); \
  NS_SCRIPTABLE NS_IMETHOD Activate(void); \
  NS_SCRIPTABLE NS_IMETHOD CanInsertField(const char *fieldType, PRBool *_retval); \
  NS_SCRIPTABLE NS_IMETHOD CursorInField(const char *fieldType, zoteroIntegrationField **_retval); \
  NS_SCRIPTABLE NS_IMETHOD GetDocumentData(PRUnichar **_retval); \
  NS_SCRIPTABLE NS_IMETHOD SetDocumentData(const PRUnichar *data); \
  NS_SCRIPTABLE NS_IMETHOD InsertField(const char *fieldType, PRUint16 noteType, zoteroIntegrationField **_retval); \
  NS_SCRIPTABLE NS_IMETHOD GetFields(const char *fieldType, nsISimpleEnumerator **_retval); \
  NS_SCRIPTABLE NS_IMETHOD Convert(nsISimpleEnumerator *fields, const char *toFieldType, PRUint16 *toNoteType, PRUint32 count); \
  NS_SCRIPTABLE NS_IMETHOD Cleanup(void); \

/* Use this macro to declare functions that forward the behavior of this interface to another object. */
#define NS_FORWARD_ZOTEROINTEGRATIONDOCUMENT(_to) \
  NS_SCRIPTABLE NS_IMETHOD DisplayAlert(const PRUnichar *dialogText, PRUint16 icon, PRUint16 buttons, PRInt16 *_retval) { return _to DisplayAlert(dialogText, icon, buttons, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Activate(void) { return _to Activate(); } \
  NS_SCRIPTABLE NS_IMETHOD CanInsertField(const char *fieldType, PRBool *_retval) { return _to CanInsertField(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD CursorInField(const char *fieldType, zoteroIntegrationField **_retval) { return _to CursorInField(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD GetDocumentData(PRUnichar **_retval) { return _to GetDocumentData(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD SetDocumentData(const PRUnichar *data) { return _to SetDocumentData(data); } \
  NS_SCRIPTABLE NS_IMETHOD InsertField(const char *fieldType, PRUint16 noteType, zoteroIntegrationField **_retval) { return _to InsertField(fieldType, noteType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD GetFields(const char *fieldType, nsISimpleEnumerator **_retval) { return _to GetFields(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Convert(nsISimpleEnumerator *fields, const char *toFieldType, PRUint16 *toNoteType, PRUint32 count) { return _to Convert(fields, toFieldType, toNoteType, count); } \
  NS_SCRIPTABLE NS_IMETHOD Cleanup(void) { return _to Cleanup(); } \

/* Use this macro to declare functions that forward the behavior of this interface to another object in a safe way. */
#define NS_FORWARD_SAFE_ZOTEROINTEGRATIONDOCUMENT(_to) \
  NS_SCRIPTABLE NS_IMETHOD DisplayAlert(const PRUnichar *dialogText, PRUint16 icon, PRUint16 buttons, PRInt16 *_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->DisplayAlert(dialogText, icon, buttons, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Activate(void) { return !_to ? NS_ERROR_NULL_POINTER : _to->Activate(); } \
  NS_SCRIPTABLE NS_IMETHOD CanInsertField(const char *fieldType, PRBool *_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->CanInsertField(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD CursorInField(const char *fieldType, zoteroIntegrationField **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->CursorInField(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD GetDocumentData(PRUnichar **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetDocumentData(_retval); } \
  NS_SCRIPTABLE NS_IMETHOD SetDocumentData(const PRUnichar *data) { return !_to ? NS_ERROR_NULL_POINTER : _to->SetDocumentData(data); } \
  NS_SCRIPTABLE NS_IMETHOD InsertField(const char *fieldType, PRUint16 noteType, zoteroIntegrationField **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->InsertField(fieldType, noteType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD GetFields(const char *fieldType, nsISimpleEnumerator **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetFields(fieldType, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Convert(nsISimpleEnumerator *fields, const char *toFieldType, PRUint16 *toNoteType, PRUint32 count) { return !_to ? NS_ERROR_NULL_POINTER : _to->Convert(fields, toFieldType, toNoteType, count); } \
  NS_SCRIPTABLE NS_IMETHOD Cleanup(void) { return !_to ? NS_ERROR_NULL_POINTER : _to->Cleanup(); } \

#if 0
/* Use the code below as a template for the implementation class for this interface. */

/* Header file */
class _MYCLASS_ : public zoteroIntegrationDocument
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_ZOTEROINTEGRATIONDOCUMENT

  _MYCLASS_();

private:
  ~_MYCLASS_();

protected:
  /* additional members */
};

/* Implementation file */
NS_IMPL_ISUPPORTS1(_MYCLASS_, zoteroIntegrationDocument)

_MYCLASS_::_MYCLASS_()
{
  /* member initializers and constructor code */
}

_MYCLASS_::~_MYCLASS_()
{
  /* destructor code */
}

/* short displayAlert (in wstring dialogText, in unsigned short icon, in unsigned short buttons); */
NS_IMETHODIMP _MYCLASS_::DisplayAlert(const PRUnichar *dialogText, PRUint16 icon, PRUint16 buttons, PRInt16 *_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void activate (); */
NS_IMETHODIMP _MYCLASS_::Activate()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* boolean canInsertField (in string fieldType); */
NS_IMETHODIMP _MYCLASS_::CanInsertField(const char *fieldType, PRBool *_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* zoteroIntegrationField cursorInField (in string fieldType); */
NS_IMETHODIMP _MYCLASS_::CursorInField(const char *fieldType, zoteroIntegrationField **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* wstring getDocumentData (); */
NS_IMETHODIMP _MYCLASS_::GetDocumentData(PRUnichar **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void setDocumentData (in wstring data); */
NS_IMETHODIMP _MYCLASS_::SetDocumentData(const PRUnichar *data)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* zoteroIntegrationField insertField (in string fieldType, in unsigned short noteType); */
NS_IMETHODIMP _MYCLASS_::InsertField(const char *fieldType, PRUint16 noteType, zoteroIntegrationField **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* nsISimpleEnumerator getFields (in string fieldType); */
NS_IMETHODIMP _MYCLASS_::GetFields(const char *fieldType, nsISimpleEnumerator **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void convert (in nsISimpleEnumerator fields, in string toFieldType, [array, size_is (count)] in unsigned short toNoteType, in unsigned long count); */
NS_IMETHODIMP _MYCLASS_::Convert(nsISimpleEnumerator *fields, const char *toFieldType, PRUint16 *toNoteType, PRUint32 count)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* void cleanup (); */
NS_IMETHODIMP _MYCLASS_::Cleanup()
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* End of implementation class template. */
#endif


/* starting interface:    zoteroIntegrationApplication */
#define ZOTEROINTEGRATIONAPPLICATION_IID_STR "7b258e57-20cf-4a73-8420-5d06a538c25e"

#define ZOTEROINTEGRATIONAPPLICATION_IID \
  {0x7b258e57, 0x20cf, 0x4a73, \
    { 0x84, 0x20, 0x5d, 0x06, 0xa5, 0x38, 0xc2, 0x5e }}

/**
 * The zoteroIntegrationApplication interface corresponds to a word processing application.
 */
class NS_NO_VTABLE NS_SCRIPTABLE zoteroIntegrationApplication : public nsISupports {
 public: 

  NS_DECLARE_STATIC_IID_ACCESSOR(ZOTEROINTEGRATIONAPPLICATION_IID)

  /* readonly attribute ACString primaryFieldType; */
  NS_SCRIPTABLE NS_IMETHOD GetPrimaryFieldType(nsACString & aPrimaryFieldType) = 0;

  /* readonly attribute ACString secondaryFieldType; */
  NS_SCRIPTABLE NS_IMETHOD GetSecondaryFieldType(nsACString & aSecondaryFieldType) = 0;

  /**
   * The active document.
   */
  /* zoteroIntegrationDocument getActiveDocument (); */
  NS_SCRIPTABLE NS_IMETHOD GetActiveDocument(zoteroIntegrationDocument **_retval) = 0;

};

  NS_DEFINE_STATIC_IID_ACCESSOR(zoteroIntegrationApplication, ZOTEROINTEGRATIONAPPLICATION_IID)

/* Use this macro when declaring classes that implement this interface. */
#define NS_DECL_ZOTEROINTEGRATIONAPPLICATION \
  NS_SCRIPTABLE NS_IMETHOD GetPrimaryFieldType(nsACString & aPrimaryFieldType); \
  NS_SCRIPTABLE NS_IMETHOD GetSecondaryFieldType(nsACString & aSecondaryFieldType); \
  NS_SCRIPTABLE NS_IMETHOD GetActiveDocument(zoteroIntegrationDocument **_retval); 

/* Use this macro to declare functions that forward the behavior of this interface to another object. */
#define NS_FORWARD_ZOTEROINTEGRATIONAPPLICATION(_to) \
  NS_SCRIPTABLE NS_IMETHOD GetPrimaryFieldType(nsACString & aPrimaryFieldType) { return _to GetPrimaryFieldType(aPrimaryFieldType); } \
  NS_SCRIPTABLE NS_IMETHOD GetSecondaryFieldType(nsACString & aSecondaryFieldType) { return _to GetSecondaryFieldType(aSecondaryFieldType); } \
  NS_SCRIPTABLE NS_IMETHOD GetActiveDocument(zoteroIntegrationDocument **_retval) { return _to GetActiveDocument(_retval); } 

/* Use this macro to declare functions that forward the behavior of this interface to another object in a safe way. */
#define NS_FORWARD_SAFE_ZOTEROINTEGRATIONAPPLICATION(_to) \
  NS_SCRIPTABLE NS_IMETHOD GetPrimaryFieldType(nsACString & aPrimaryFieldType) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetPrimaryFieldType(aPrimaryFieldType); } \
  NS_SCRIPTABLE NS_IMETHOD GetSecondaryFieldType(nsACString & aSecondaryFieldType) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetSecondaryFieldType(aSecondaryFieldType); } \
  NS_SCRIPTABLE NS_IMETHOD GetActiveDocument(zoteroIntegrationDocument **_retval) { return !_to ? NS_ERROR_NULL_POINTER : _to->GetActiveDocument(_retval); } 

#if 0
/* Use the code below as a template for the implementation class for this interface. */

/* Header file */
class _MYCLASS_ : public zoteroIntegrationApplication
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_ZOTEROINTEGRATIONAPPLICATION

  _MYCLASS_();

private:
  ~_MYCLASS_();

protected:
  /* additional members */
};

/* Implementation file */
NS_IMPL_ISUPPORTS1(_MYCLASS_, zoteroIntegrationApplication)

_MYCLASS_::_MYCLASS_()
{
  /* member initializers and constructor code */
}

_MYCLASS_::~_MYCLASS_()
{
  /* destructor code */
}

/* readonly attribute ACString primaryFieldType; */
NS_IMETHODIMP _MYCLASS_::GetPrimaryFieldType(nsACString & aPrimaryFieldType)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* readonly attribute ACString secondaryFieldType; */
NS_IMETHODIMP _MYCLASS_::GetSecondaryFieldType(nsACString & aSecondaryFieldType)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* zoteroIntegrationDocument getActiveDocument (); */
NS_IMETHODIMP _MYCLASS_::GetActiveDocument(zoteroIntegrationDocument **_retval)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* End of implementation class template. */
#endif


#endif /* __gen_zoteroIntegration_h__ */

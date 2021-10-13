const documentExts = [
  'doc',
  'docx',
  'docm',
  'dot',
  'dotx',
  'dotm',
  'odt',
  'fodt',
  'ott',
  'rtf',
  'txt',
  'html',
  'htm',
  'mht',
  'xml',
  'pdf',
  'djvu',
  'fb2',
  'epub',
  'xps',
];

const spreadsheetExts = [
  'xls',
  'xlsx',
  'xlsm',
  'xlt',
  'xltx',
  'xltm',
  'ods',
  'fods',
  'ots',
  'csv',
];

const presentationExts = [
  'pps',
  'ppsx',
  'ppsm',
  'ppt',
  'pptx',
  'pptm',
  'pot',
  'potx',
  'potm',
  'odp',
  'fodp',
  'otp',
];

export const getFileType = function (fileName: string) {
  const word = 'word';
  const cell = 'cell';
  const slide = 'slide';

  const ext = fileName.split('.')[1];

  if (documentExts.indexOf(ext) != -1) return word;
  if (spreadsheetExts.indexOf(ext) != -1) return cell;
  if (presentationExts.indexOf(ext) != -1) return slide;

  return word;
};

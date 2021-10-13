export function Download(slackLink: string, fileName: string) {
  const base_url = 'https://files.slack.com/files-pri/';
  const ids = slackLink.split('/').pop()?.split('-');
  const pub_secret = ids?.pop();

  const downloadUrl =
    base_url +
    ids[0] +
    '-' +
    ids[1] +
    `/download/${fileName}?pub_secret=${pub_secret}`;

  return downloadUrl;
}

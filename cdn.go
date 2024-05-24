package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/aliyun/alibaba-cloud-sdk-go/services/cdn"
	"os"
	"regexp"
	"strings"
)

type Config struct {
	AccessKeyId     string
	AccessKeySecret string
	RegionId        string
	TaskType        string
	FilePath        string
	ObjectType      string
	Area            string
}

func main() {
	config := parseArgs()

	client, err := cdn.NewClientWithAccessKey(config.RegionId, config.AccessKeyId, config.AccessKeySecret)
	if err != nil {
		fmt.Println("Error creating Alibaba Cloud client:", err)
		return
	}

	urls, err := readUrls(config.FilePath)
	if err != nil {
		fmt.Println("Error reading URLs from file:", err)
		return
	}

	switch config.TaskType {
	case "clear":
		err = refreshUrls(client, urls, config.ObjectType)
	case "push":
		err = preheatUrls(client, urls, config.Area)
	default:
		fmt.Println("Invalid task type. Use 'clear' or 'push'.")
		return
	}

	if err != nil {
		fmt.Println("Error executing task:", err)
	}
}

func parseArgs() *Config {
	config := &Config{}
	flag.StringVar(&config.AccessKeyId, "i", "", "AccessKeyId")
	flag.StringVar(&config.AccessKeySecret, "k", "", "AccessKeySecret")
	flag.StringVar(&config.FilePath, "r", "", "FilePath")
	flag.StringVar(&config.TaskType, "t", "", "TaskType (clear or push)")
	flag.StringVar(&config.ObjectType, "o", "File", "ObjectType (File or Directory)")
	flag.StringVar(&config.Area, "a", "global", "Area (domestic or overseas)")
	flag.StringVar(&config.RegionId, "region", "cn-hangzhou", "RegionId (default is cn-hangzhou)")
	flag.Parse()

	if config.AccessKeyId == "" || config.AccessKeySecret == "" || config.FilePath == "" || config.TaskType == "" {
		fmt.Println("Missing required arguments")
		flag.Usage()
		os.Exit(1)
	}

	return config
}

func readUrls(filePath string) ([]string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var urls []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		url := scanner.Text()
		if isValidUrl(url) {
			urls = append(urls, url)
		} else {
			fmt.Printf("Invalid URL format: %s\n", url)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return urls, nil
}

func isValidUrl(url string) bool {
	re := regexp.MustCompile(`^https?://`)
	return re.MatchString(url)
}

func refreshUrls(client *cdn.Client, urls []string, objectType string) error {
	request := cdn.CreateRefreshObjectCachesRequest()
	request.Scheme = "https"
	request.ObjectPath = strings.Join(urls, "\n")
	request.ObjectType = objectType

	response, err := client.RefreshObjectCaches(request)
	if err != nil {
		return err
	}

	printResponse(response.GetHttpStatus(), response.GetHttpContentString())
	return nil
}

func preheatUrls(client *cdn.Client, urls []string, area string) error {
	request := cdn.CreatePushObjectCacheRequest()
	request.Scheme = "https"
	request.ObjectPath = strings.Join(urls, "\n")
	request.Area = area

	response, err := client.PushObjectCache(request)
	if err != nil {
		return err
	}

	printResponse(response.GetHttpStatus(), response.GetHttpContentString())
	return nil
}

func printResponse(status int, response string) {
	fmt.Printf("HTTP Status: %d\n", status)
	var prettyJSON bytes.Buffer
	if err := json.Indent(&prettyJSON, []byte(response), "", "    "); err != nil {
		fmt.Println("Failed to format JSON:", err)
		return
	}
	fmt.Println(prettyJSON.String())
}
